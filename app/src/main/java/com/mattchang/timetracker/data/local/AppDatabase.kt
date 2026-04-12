package com.mattchang.timetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mattchang.timetracker.data.local.converter.Converters
import com.mattchang.timetracker.data.local.dao.CategoryDao
import com.mattchang.timetracker.data.local.dao.TagDao
import com.mattchang.timetracker.data.local.dao.TimeRecordDao
import com.mattchang.timetracker.data.local.entity.CategoryEntity
import com.mattchang.timetracker.data.local.entity.RecordTagCrossRef
import com.mattchang.timetracker.data.local.entity.TagEntity
import com.mattchang.timetracker.data.local.entity.TimeRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TimeRecordEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        RecordTagCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun timeRecordDao(): TimeRecordDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao

    companion object {
        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "time_tracker.db"
            )
                .addCallback(SeedCallback())
                .build()
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                seedCategories(db)
            }
        }

        private fun seedCategories(db: SupportSQLiteDatabase) {
            val categories = listOf(
                Triple("工作", "work", "#4CAF50"),
                Triple("學習", "school", "#2196F3"),
                Triple("運動", "fitness_center", "#FF9800"),
                Triple("休息", "weekend", "#9C27B0"),
                Triple("通勤", "directions_bus", "#607D8B"),
                Triple("家務", "home", "#795548"),
                Triple("娛樂", "sports_esports", "#E91E63"),
                Triple("社交", "people", "#00BCD4"),
                Triple("閱讀", "menu_book", "#8BC34A"),
                Triple("睡眠", "bedtime", "#3F51B5"),
                Triple("陪伴家人", "family_restroom", "#FF5722"),
                Triple("其他", "more_horiz", "#9E9E9E"),
            )

            categories.forEachIndexed { index, (name, icon, color) ->
                db.execSQL(
                    "INSERT INTO categories (name, icon, color_hex, is_default, sort_order) VALUES (?, ?, ?, 1, ?)",
                    arrayOf(name, icon, color, index)
                )
            }
        }
    }
}
