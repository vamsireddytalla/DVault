package com.talla.dvault.modules

import android.content.Context
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import androidx.annotation.Nullable
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.talla.dvault.R
import com.talla.dvault.database.VaultDatabase
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.utills.RealPathUtill
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Singleton

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

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