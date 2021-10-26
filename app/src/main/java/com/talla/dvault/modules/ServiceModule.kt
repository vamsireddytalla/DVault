package com.talla.dvault.modules

import android.content.Context
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import com.talla.dvault.database.VaultDatabase
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.utills.RealPathUtill
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule
{

    @Provides
    @ServiceScoped
    fun getAppDB(@ApplicationContext context: Context): VaultDatabase {
        return VaultDatabase.invoke(context.applicationContext)
    }

    @Provides
    @ServiceScoped
    fun getDVaultDao(appDB: VaultDatabase): DVaultDao {
        return appDB.vaulDao()
    }


}