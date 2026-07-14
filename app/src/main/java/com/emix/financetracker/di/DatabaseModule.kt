package com.emix.financetracker.di

import android.content.Context
import androidx.room.Room
import com.emix.financetracker.data.db.AppDatabase
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "finance_tracker.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: AppDatabase) = database.budgetDao()

    @Provides
    @Singleton
    fun provideProductsMonthDao(database: AppDatabase) = database.productsMonthDao()

    @Provides
    @Singleton
    fun providePurchaseHistoryDao(database: AppDatabase) = database.purchaseHistoryDao()
}