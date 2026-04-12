package com.mattchang.timetracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mattchang.timetracker.MainActivity
import com.mattchang.timetracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the timer alive when the app goes to the background.
 * The actual timer state (startEpochMillis) is owned by TimerViewModel + SavedStateHandle;
 * this service only drives the notification display.
 */
class TimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickJob: Job? = null
    private var startEpochMillis: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startEpochMillis = intent.getLongExtra(EXTRA_START_EPOCH, System.currentTimeMillis())
                startForeground(NOTIFICATION_ID, buildNotification(0L))
                startTick()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTick() {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            while (true) {
                delay(1000L)
                val elapsed = (System.currentTimeMillis() - startEpochMillis) / 1000L
                updateNotification(elapsed)
            }
        }
    }

    private fun updateNotification(elapsedSeconds: Long) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(elapsedSeconds))
    }

    private fun buildNotification(elapsedSeconds: Long): Notification {
        val h = elapsedSeconds / 3600
        val m = (elapsedSeconds % 3600) / 60
        val s = elapsedSeconds % 60
        val timeStr = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(getString(R.string.timer_running))
            .setContentText(timeStr)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the running timer"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        tickJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.mattchang.timetracker.TIMER_START"
        const val ACTION_STOP  = "com.mattchang.timetracker.TIMER_STOP"
        const val EXTRA_START_EPOCH = "start_epoch"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "timer_channel"

        fun startIntent(context: Context, startEpochMillis: Long): Intent =
            Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_START_EPOCH, startEpochMillis)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
