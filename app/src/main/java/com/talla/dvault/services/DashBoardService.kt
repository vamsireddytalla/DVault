package com.talla.dvault.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.talla.dvault.R
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

private const val TAG = "DashBoardService"

@AndroidEntryPoint
class DashBoardService : Service() {
    @Inject
    lateinit var repository: VaultRepository
    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): DashBoardService = this@DashBoardService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        startServiceOreoCondition()
        GlobalScope.async {
            startServiceOreoCondition()
//            getDriveFilesAndSearchDb()
        }
    }

    fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(this)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                this, listOf(DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = googleAccount.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(this.getString(R.string.app_name))
                .build()
        }
        return null
    }

    fun getDriveFilesAndSearchDb() {
        getDriveService()?.let { gdService ->
            var pagetoken: String? = null
            do {
                val result = gdService.files().list().apply {
                    spaces = "appDataFolder"
                    fields = "nextPageToken, files(id, name,mimeType)"
                    q = "mimeType='application/vnd.google-apps.folder'"
                    pageToken = this.pageToken
                }.execute()
                deleteDatabaseFile("DVault.db")
                result?.let { res ->
                    res.files.forEach { file ->
                        Log.d(" DashBoard FILE", " ${file.name} ${file.id} ${file.mimeType}")
//                        downloadDataBase(file.id)
                    }
                }

            } while (pagetoken != null)
        }
    }

    private fun deleteDatabaseFile(databaseName: String) {
        val databases = File(this.applicationInfo.dataDir.toString() + "/databases")
        val db = File(databases, databaseName)
        if (db.delete()) Log.d(TAG, "deleteDatabaseFile: Deleted ") else Log.d(TAG, "deleteDatabaseFile: Not-Deleted ")
        val shm = File(databases, "$databaseName-shm")
        if (shm.exists()) {
            if (shm.delete()) Log.d(TAG, "deleteDatabaseFile: Deleted Shm ") else Log.d(TAG, "deleteDatabaseFile: Not-Deleted shm")
        }
        val wal = File(databases, "$databaseName-wal")
        if (wal.exists()) {
            if (wal.delete()) Log.d(TAG, "deleteDatabaseFile: Deleted wal ") else Log.d(TAG, "deleteDatabaseFile: Not-Deleted wal")
        }
    }

    fun downloadDataBase(fileId: String) {
        val outputStream: OutputStream = ByteArrayOutputStream()
        getDriveService()?.let {
            it.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        }
    }


    private fun startServiceOreoCondition() {
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "890"
            val CHANNEL_NAME = "DASHBOARD_SERVICE"
            val channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.drawable.notification_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setNotificationSilent()
                .build()
            startForeground(888, notification)
        }
    }

}