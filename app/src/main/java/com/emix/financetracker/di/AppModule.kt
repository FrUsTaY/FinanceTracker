package com.emix.financetracker.di

import android.content.Context
import com.emix.financetracker.data.db.dao.BudgetDao
import com.emix.financetracker.data.db.dao.ProductsMonthDao
import com.emix.financetracker.data.network.TokenManager
import com.emix.financetracker.data.repository.BudgetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: BudgetDao,
        productsMonthDao: ProductsMonthDao
    ): BudgetRepository = BudgetRepository(budgetDao, productsMonthDao)

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager = TokenManager(context)
}