package com.talla.dvault.modules

import android.app.Application
import android.app.Dialog
import android.content.ContentResolver
import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.talla.dvault.R
import com.talla.dvault.database.VaultDatabase
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.databinding.CustonProgressDialogBinding
import com.talla.dvault.preferences.UserPreferences
import com.talla.dvault.repositories.AppLockRepository
import com.talla.dvault.repositories.VaultRepository
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
    fun getContentResolver(@ApplicationContext context: Context):ContentResolver
    {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun getAppDB(@ApplicationContext context: Context): VaultDatabase {
        return VaultDatabase.invoke(context.applicationContext)
    }

    @Provides
    @Singleton
    fun getDVaultDao(appDB: VaultDatabase): DVaultDao {
        return appDB.vaulDao()
    }

    @Provides
    @Singleton
    fun provideMainRepository(appDao: DVaultDao): VaultRepository {
        return VaultRepository(appDao)
    }

    @Provides
    @Singleton
    fun provideAppLockRepository(appDao: DVaultDao): AppLockRepository {
        return AppLockRepository(appDao)
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context):RequestManager {
        return  Glide.with(context).setDefaultRequestOptions(
            RequestOptions()
                .placeholder(R.drawable.ic_skaleten)
//            .error(R.drawable.ic_image)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
        )
    }

}