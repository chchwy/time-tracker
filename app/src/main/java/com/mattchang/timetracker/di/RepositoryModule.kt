package com.mattchang.timetracker.di

import com.mattchang.timetracker.data.repository.CategoryRepositoryImpl
import com.mattchang.timetracker.data.repository.TagRepositoryImpl
import com.mattchang.timetracker.data.repository.TimeRecordRepositoryImpl
import com.mattchang.timetracker.domain.repository.CategoryRepository
import com.mattchang.timetracker.domain.repository.TagRepository
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTimeRecordRepository(
        impl: TimeRecordRepositoryImpl
    ): TimeRecordRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(
        impl: TagRepositoryImpl
    ): TagRepository
}
