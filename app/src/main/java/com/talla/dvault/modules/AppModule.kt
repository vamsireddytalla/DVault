package com.talla.dvault.modules

import android.app.Application
import android.content.Context
import com.talla.dvault.database.VaultDatabase
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.repositories.VaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule
{

    @Provides
    @Singleton
    fun getAppDB(@ApplicationContext context: Context):VaultDatabase
    {
        return VaultDatabase.invoke(context.applicationContext)
    }

    @Provides
    @Singleton
    fun getDVaultDao(appDB:VaultDatabase):DVaultDao{
        return appDB.vaulDao()
    }

    @Provides
    @Singleton
    fun provideMainRepository(appDao:DVaultDao):VaultRepository{
        return VaultRepository(appDao)
    }

}