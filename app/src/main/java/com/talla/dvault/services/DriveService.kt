package com.talla.dvault.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.About
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.google.api.services.drive.model.File;
import com.talla.dvault.R
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.utills.FileSize
import java.util.*
import com.google.api.client.http.FileContent
import java.lang.Exception
import com.google.api.client.googleapis.media.MediaHttpDownloader
import com.google.api.client.googleapis.media.MediaHttpDownloader.DownloadState

import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.media.MediaHttpUploader.UploadState

import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.InputStreamContent
import com.talla.dvault.exceptions.GoogleDriveException
import kotlinx.coroutines.*
import java.io.*
import kotlin.collections.ArrayList
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.model.FileList
import com.talla.dvault.database.entities.CategoriesModel


private const val TAG = "DriveService"

@AndroidEntryPoint
class DriveService : Service() {
    @Inject
    lateinit var repository: VaultRepository
    private val binder: IBinder = LocalBinder()
    private lateinit var notificationLayout: RemoteViews
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification
    private lateinit var builder: NotificationCompat.Builder
    private var settingsCallbackListner: SettingsCalBack? = null
    private lateinit var itemModelList: ArrayList<ItemModel>
    private var isInterrupted: Boolean? = false
    private var allItemFileSize: Double = 0.0
    private var cancelProcessJob: Job? = null
    private var mediaContent: FileContent? = null
    private val backgroundScope = CoroutineScope(Dispatchers.Default)


    override fun onCreate() {
        super.onCreate()
        startServiceOreoCondition()
        isInterrupted = false
        backgroundScope.launch {
            getTotalDriveStorages()
//            getDriveFiles()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val actionMaker = intent.action
            itemModelList = ArrayList<ItemModel>()
            itemModelList.clear()
            if (intent.extras != null) {
                val type: Any? = intent.extras!!.get(this.resources.getString(R.string.key))
                when (actionMaker) {
                    FileSize.ACTION_SETTINGS_START_FOREGROUND_SERVICE -> {
                        Log.d(TAG, "onStartCommand: Started Service")
                        cancelProcessJob = backgroundScope.launch {
                            var selecteData = FileSize.selectedBackRestore
                            Log.d(TAG, "onStartCommand: ${selecteData}")
                            selecteData.forEach { catType ->
                                Log.d(TAG, "onStartCommand: $catType")
                                if (type==this@DriveService.resources.getString(R.string.backup))
                                {
                                    itemModelList = repository.getBRItems(catType) as ArrayList<ItemModel>
                                }else{
                                    itemModelList = repository.getRBItems(catType) as ArrayList<ItemModel>
                                }

                            }
                            Log.d(TAG, "onStartCommand: Button Type -----> $type")
                            Log.d(TAG, "onStartCommand: Total List Received ${itemModelList.toString()}")
                            if (itemModelList.isEmpty()) {
                                Log.d(TAG, "onStartCommand: No Items Found")
                                stopServiceMethod(type.toString()+" Completed!")
                            } else {
                                isInterrupted = false
                                createNotification(
                                    FileSize.BR_CHANNEl_ID,
                                    FileSize.BR_CHANNEL_NAME,
                                    FileSize.BR_NOTIFY_ID,
                                    FileSize.ACTION_SETTINGS_STOP_FOREGROUND_SERVICE
                                )
                                for ((index, source) in itemModelList.withIndex()) {
                                    if (!isInterrupted!!) {
                                       try {
                                           if (source.serverId == null || source.serverId.isEmpty()) {
                                               try {
                                                   uploadLargeFiles(source, index + 1)
                                               } catch (e: Exception) {
                                                   e.printStackTrace()
                                                   Log.d(TAG, "onStartCommand: Exception Occured-----> ${e.message}")
                                               }
                                           } else {
                                               val orgDir: java.io.File = this@DriveService.getDir(source.itemCatType, Context.MODE_PRIVATE)
                                               val searchFile=File(orgDir.toString()+"/"+source.itemName)
                                               Log.d(TAG, "onStartCommand: Secret File Path $searchFile")
                                               if (searchFile.exists()) {
                                                   Log.d(TAG, "onStartCommand: File Already Exists")
                                                   continue
                                               }
                                               downloadDBFiles(source, index + 1,searchFile.toString())
                                           }
                                       }catch (e:Exception){
                                           e.printStackTrace()
                                           Log.d(TAG, "onStartCommand: ${e.message}")
                                       }
                                    } else break
                                }
                                stopServiceMethod(type.toString()+" Completed!")
                                cancelProcessJob?.join()
                            }
                        }
                    }
                    FileSize.ACTION_SETTINGS_STOP_FOREGROUND_SERVICE -> {
                        Log.d(TAG, "onStartCommand: Stopped Foreground Service")
                        cancelProcessJob?.let {
                            isInterrupted = true
                            mediaContent?.closeInputStream
                            it.cancel()
                        }
                        FileSize.backUpRestoreEnabled = false
                        settingsCallbackListner?.fileServerDealing(0, type.toString(), "")
                        stopForeground(false)
                        notificationManager.cancel(FileSize.BR_NOTIFY_ID)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun updateDbFiles(serverid: String, catName: String, catType: String) {
        try {

            var mimeType = catType
            // File's new content.
            val file = File(this.resources.getString(R.string.db_path) + catName)
            val newMetadata = com.google.api.services.drive.model.File()
            newMetadata.name = file.name

            // Convert content to an AbstractInputStreamContent instance.
            val contentStream = ByteArrayContent.fromString(mimeType, file.toString())
            val mediaContentNew =
                InputStreamContent(catType, BufferedInputStream(FileInputStream(file)))
            // Send the request to the API.
            var fileRes = getDriveService()?.let {
                it.files().update(serverid, newMetadata, mediaContentNew)
                    .setFields("id, name, appProperties").execute()
            }
            fileRes?.let {
                Log.d(TAG, "UpdateDbFiles: Successfully ${fileRes.name} ${fileRes.id}")
            }
        } catch (e: IOException) {
            println("An error occurred: $e")
            Log.d(TAG, "UpdateDbFile Error --> : ${e.message}")

        }
    }

    fun stopServiceMethod(message: String) {
        Log.d(TAG, "stopServiceMethod: Called")
        settingsCallbackListner?.fileServerDealing(0, message, "")
        var clickIntent = Intent(this@DriveService, DriveService::class.java)
        clickIntent.action = FileSize.ACTION_SETTINGS_STOP_FOREGROUND_SERVICE
        clickIntent.putExtra(this@DriveService.resources.getString(R.string.key),message)
        startService(clickIntent)
    }

    inner class LocalBinder : Binder() {
        fun getService(): DriveService = this@DriveService

        fun settingsBRCallback(callback: SettingsCalBack) {
            Log.d(TAG, "SettingsCallback : Called")
            settingsCallbackListner = callback
        }

        fun startBackUpService(btnType:String) {
            var clickIntent = Intent(this@DriveService, DriveService::class.java)
            clickIntent.action = FileSize.ACTION_SETTINGS_START_FOREGROUND_SERVICE
            clickIntent.putExtra(this@DriveService.resources.getString(R.string.key),btnType)
            startService(clickIntent)
        }

        fun stopSettingsService(cancelMsg:String) {
            stopServiceMethod(cancelMsg)
        }

        fun getDriveStorage(){
            getTotalDriveStorages()
        }
    }

    fun getDriveFiles() {
        getDriveService()?.let { gdService ->
            var pagetoken: String? = null
            do {
                val result = gdService.files().list().apply {
                    spaces = "appDataFolder"
                    fields = "nextPageToken, files(id,name,mimeType,quotaBytesUsed)"
                    var mimeType =
                        this@DriveService.resources.getString(R.string.file_mime_type)
//                    q = "mimeType='$mimeType'"
                    pageToken = this.pageToken
                }.execute()

                result?.let { res ->
                    res.files.forEach { file ->
                        Log.d("FILE", " ${file.name} ${file.id} ${file.mimeType} ${FileSize.bytesToHuman(file.quotaBytesUsed)}")
//                        deleteData(file.id)
                    }
                }

            } while (pagetoken != null)
        }
    }

    fun deleteData(fileId: String) {
        val files = getDriveService()?.let {
            it.files().delete(fileId).execute()
        }
    }

    fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(this)?.let { googleAccount ->
            val credential =
                GoogleAccountCredential.usingOAuth2(this, listOf(DriveScopes.DRIVE_APPDATA))
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

    fun createFolder() {
        val fileMetadata = File()
        fileMetadata.name = "Vamsi Folder"
        fileMetadata.parents = Collections.singletonList("appDataFolder")
        fileMetadata.mimeType = this.resources.getString(R.string.folder_mime_type)

        val file: File? = getDriveService()?.let {
            it.files().create(fileMetadata)
                .setFields("id")
                .execute()
        }
        Log.d(TAG, "Created New Folder : ${file?.id} ${file?.name}")
        file?.let {
//            uploadData(it.id)
            getDriveFiles()
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun startServiceOreoCondition() {
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "889"
            val CHANNEL_NAME = "SETTINGS_SERVICE"
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.drawable.notification_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setNotificationSilent()
                .build()
            startForeground(888, notification)
        }
    }

    //quotaBytesUsed
    private fun updateDbFiles(service: Drive, fileId: String, fileName: String) {
        try {
            var mimeType = "file/*"
            // File's new content.
            val file = File(this.resources.getString(R.string.db_path) + fileName)
            val newMetadata = com.google.api.services.drive.model.File()
            newMetadata.name = file.name

            // Convert content to an AbstractInputStreamContent instance.
            val contentStream = ByteArrayContent.fromString(mimeType, file.toString())
            val mediaContentNew = InputStreamContent("file/*", BufferedInputStream(FileInputStream(file)))
            // Send the request to the API.
            var fileRes = service.files().update(fileId, newMetadata, mediaContentNew).setFields("id, name, appProperties,quotaBytesUsed").execute()
            fileRes?.let {
                Log.d(TAG, "UpdateDbFiles: Successfully ${fileRes.name} ${fileRes.id} ${fileRes.quotaBytesUsed}")
            }
        } catch (e: IOException) {
            println("An error occurred: $e")
            Log.d(TAG, "UpdateDbFile Error --> : ${e.message}")
        }
    }

    fun downloadDBFiles(itemModel: ItemModel, itemNo: Int,oriPath:String) {

        val out: OutputStream = FileOutputStream(oriPath)
        val request: Drive.Files.Get? = getDriveService()?.files()?.get(itemModel.serverId)
        request?.let {
            val uploader: MediaHttpDownloader = it.mediaHttpDownloader
            uploader.isDirectDownloadEnabled = false
            uploader.chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE
            val downloadListner = CustomProgressListener(itemModel, itemNo, FileSize.bytesToHuman(itemModel.itemSize.toLong()).toString(),oriPath.toString())
            it.mediaHttpDownloader?.progressListener = downloadListner
            it.executeMediaAndDownloadTo(out)
        }
    }

    suspend fun uploadLargeFiles(itemModel: ItemModel, itemNo: Int) {
        val fileMetadata = File()
        fileMetadata.name = itemModel.itemName
        val catModel = repository.getDbServerFolderId(itemModel.itemCatType)
        fileMetadata.parents = Collections.singletonList(catModel.serverId)
        fileMetadata.mimeType = FileSize.getMimeType(itemModel.itemCurrentPath)

        val file: java.io.File = File(itemModel.itemCurrentPath)
        mediaContent = FileContent(FileSize.getMimeType(file.toString()), file)
        val fileSize: String? = FileSize.bytesToHuman(file.length())
        Log.d(TAG, "ItemSize : $fileSize ")
        val res: Drive.Files.Create? = getDriveService()?.let {
            it.files().create(fileMetadata, mediaContent)
        }
        val responsee: File? = res?.let {
            val uploader: MediaHttpUploader = it.mediaHttpUploader
            uploader.isDirectUploadEnabled = false
            Log.d(TAG, "uploadLargeFiles: Process One")
            uploader.chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE
            val listner: CustomUploadProgressListener =
                CustomUploadProgressListener(itemModel, itemNo, fileSize!!)
            it.mediaHttpUploader?.progressListener = listner
            Log.d(TAG, "uploadLargeFiles: Process Two")
            it.execute()
        }

        responsee?.let {
            Log.d(TAG, "uploadLargeFiles: ServerFile ID->  ${it.id}")
            var res = repository.updateItemServerId(it.id, itemModel.itemId)
            Log.d(TAG, "uploadLargeFiles: $res")
        }
    }

    inner class CustomProgressListener(
        var itemModel: ItemModel,
        var itemNo: Int,
        var itemSize: String,
        var downloadPath:String
    ) : MediaHttpDownloaderProgressListener {
        override fun progressChanged(downloader: MediaHttpDownloader) {
            when (downloader.downloadState) {
                DownloadState.MEDIA_IN_PROGRESS -> {
                    Log.d(TAG, "progressChanged: ${downloader.progress * 100}")
                    if (!isInterrupted!!) {
                        val respo: Double = downloader.progress * 100

                        Log.d(TAG, "progressChanged: ${respo}%")
                        val mbCount = FileSize.bytesToHuman(downloader.numBytesDownloaded)
                        Log.d(TAG, "progressChanged: Download Bytes Done $mbCount")
                        val totalItems = itemModelList.size
                        settingsCallbackListner?.fileServerDealing(
                            respo.toInt(),
                            "$mbCount/$itemSize",
                            "$itemNo/$totalItems")
                        updateNotification(
                            respo.toInt(),
                            "$mbCount/$itemSize",
                            "$itemNo/$totalItems")
                        FileSize.backUpRestoreEnabled = true
                    } else {
                        var deleteFile=File(downloadPath)
                        if (deleteFile.exists()) deleteFile.delete()
                        throw GoogleDriveException("Download canceled")
                    }
                }
                DownloadState.MEDIA_COMPLETE -> {
                    Log.d(TAG, "progressChanged: Download Is Complete")
                    Log.d(TAG, "progressChanged: TotalList ${itemModelList.size}")
                    Log.d(TAG, "progressChanged: ItemNo ${itemNo}")
                    if ((itemModelList.size) == itemNo) {
                        stopServiceMethod("Restore Completed!")
                    }
                }
            }
        }
    }

    inner class CustomUploadProgressListener(
        var itemModel: ItemModel,
        var itemNo: Int,
        var itemSize: String
    ) :
        MediaHttpUploaderProgressListener {
        @Throws(IOException::class)
        override fun progressChanged(uploader: MediaHttpUploader) {
            when (uploader.uploadState) {

                UploadState.INITIATION_STARTED -> {
                    Log.d(TAG, "progressChanged: Initiation has started!")
                }
                UploadState.INITIATION_COMPLETE -> Log.d(
                    TAG, "progressChanged: Initiation is complete!"
                )
                UploadState.MEDIA_IN_PROGRESS -> {
                    if (!isInterrupted!!) {
                        val respo: Double = uploader.progress * 100

                        Log.d(TAG, "progressChanged: ${respo}%")
                        val mbCount = FileSize.bytesToHuman(uploader.numBytesUploaded)
                        Log.d(TAG, "progressChanged: Upload Byte Done $mbCount")
                        val totalItems = itemModelList.size
                        settingsCallbackListner?.fileServerDealing(
                            respo.toInt(),
                            "$mbCount/$itemSize",
                            "$itemNo/$totalItems"
                        )
                        updateNotification(
                            respo.toInt(),
                            "$mbCount/$itemSize",
                            "$itemNo/$totalItems"
                        )
                        FileSize.backUpRestoreEnabled = true
                    } else {
                        throw GoogleDriveException("Upload canceled")
                    }

                }
                UploadState.MEDIA_COMPLETE -> {
                    Log.d(TAG, "progressChanged: Upload is complete!")
                    Log.d(TAG, "progressChanged: TotalList ${itemModelList.size}")
                    Log.d(TAG, "progressChanged: ItemNo ${itemNo}")
                    if ((itemModelList.size) == itemNo) {
                       runBlocking {
                           val catDbLocalFileList = repository.getCategoriesIfNotEmpty()
                           Log.d(TAG, "uploadData: ${catDbLocalFileList.toString()}")
                           Log.d(TAG, "uploadData: CatList Check Server Ids List ${catDbLocalFileList.size}")
                           var updateJob = backgroundScope.launch(Dispatchers.IO) {
                               catDbLocalFileList.forEach {
                                   updateDbFiles(getDriveService()!!, it.serverId, it.catId)
                               }
                           }
                           updateJob.join()
                           stopServiceMethod("Back-up Completed!")
                       }

                    }
                }
                UploadState.NOT_STARTED -> Log.d(
                    TAG,
                    "progressChanged: Upload Still Not Started"
                )
            }
        }
    }

    fun getTotalDriveStorages() {
        val about: About? = getDriveService()?.let {
            it.about().get().setFields("user, storageQuota").execute()
        }
        about?.let {
            var usedStorage = FileSize.bytesToHuman(about.storageQuota.usage)
            var totalStorage = FileSize.bytesToHuman(about.storageQuota.limit)
            settingsCallbackListner?.storageQuote(usedStorage.toString(), totalStorage.toString())
            Log.d("MainActivity", "getTotalDriveStorages: ${usedStorage.toString()}, ${totalStorage.toString()}")
        }
//        getDriveFiles()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(chan)
        return channelId
    }

    @SuppressLint("RemoteViewLayout")
    private fun createNotification(
        channelId: String,
        channelName: String,
        file_notify_id: Int,
        cancelAction: String
    ) {
        Log.d(TAG, "Settings createNotification: Called")
        //createNotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelId, channelName)
        }
        // Create notification default intent.
        val intent = Intent()
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)


        // Get the layouts to use in the custom notification
        notificationLayout = RemoteViews(packageName, R.layout.collapsed_item_progress)
        notificationLayout.setProgressBar(R.id.progressFile, 0, 0, true)
//        val notificationLayoutExpanded = RemoteViews(packageName, R.layout.notification_large)

        var clickIntent = Intent(this, DriveService::class.java)
        clickIntent.action = cancelAction
        var filePendingIntent =
            PendingIntent.getService(this, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationLayout.setOnClickPendingIntent(R.id.cancelFileProcess, filePendingIntent)

        // Create notification builder.
        builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setOnlyAlertOnce(true)
//            .setCustomBigContentView(notificationLayoutExpanded)

        //pending main intent
//        val mainIntent = Intent(baseContext, DashBoardActivity::class.java)
//        val pendingMainIntent = PendingIntent.getActivity(baseContext, 0, mainIntent, 0)
//        builder.setContentIntent(pendingMainIntent)

        // Build the notification.
        notification = builder.build()
        startForeground(file_notify_id, notification)
    }

    private fun updateNotification(
        progress: Int,
        mbCopied: String,
        totalCount: String
    ) {
        val api = Build.VERSION.SDK_INT
        // update the icon
        notificationLayout.setProgressBar(R.id.progressFile, 100, progress, false)
        // update the title
        notificationLayout.setTextViewText(R.id.totalElapsed, mbCopied)
        notificationLayout.setTextViewText(R.id.totalCount, totalCount)

        settingsCallbackListner?.fileServerDealing(progress, mbCopied, totalCount)
        if (FileSize.settingsBRSelected == this.resources.getString(R.string.backup)) {
            notificationLayout.setTextViewText(R.id.addingVaultTitle, "Backup Processing...")
        } else {
            notificationLayout.setTextViewText(R.id.addingVaultTitle, "Restore Processing...")
        }

        // update the notification
        if (api < Build.VERSION_CODES.HONEYCOMB) {
            notificationManager.notify(FileSize.BR_NOTIFY_ID, notification)
        } else if (api >= Build.VERSION_CODES.HONEYCOMB) {
            notificationManager.notify(FileSize.BR_NOTIFY_ID, builder.build())
        }

    }

    interface SettingsCalBack {
        fun fileServerDealing(progress: Int, mbCount: String, totalItems: String)
        fun storageQuote(totalStorage: String, usedStorage: String)
    }

}