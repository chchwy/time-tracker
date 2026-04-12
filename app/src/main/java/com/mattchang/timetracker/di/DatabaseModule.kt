package com.mattchang.timetracker.di

import android.content.Context
import com.mattchang.timetracker.data.local.AppDatabase
import com.mattchang.timetracker.data.local.dao.CategoryDao
import com.mattchang.timetracker.data.local.dao.TagDao
import com.mattchang.timetracker.data.local.dao.TimeRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.buildDatabase(context)
    }

    @Provides
    fun provideTimeRecordDao(database: AppDatabase): TimeRecordDao {
        return database.timeRecordDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao {
        return database.tagDao()
    }
}
