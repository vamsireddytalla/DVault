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
import com.talla.dvault.activities.SettingsActivity
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.relations.FolderAndItem


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
    private lateinit var gDriveService: Drive


    override fun onCreate() {
        super.onCreate()
        startServiceOreoCondition()
        isInterrupted = false
        backgroundScope.launch {
//            getFilesUnderParticularFolder()
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
                val bkpString = this@DriveService.resources.getString(R.string.backup)
                when (actionMaker) {
                    FileSize.ACTION_SETTINGS_START_FOREGROUND_SERVICE -> {
                        Log.d(TAG, "onStartCommand: Started Service")
                        cancelProcessJob = backgroundScope.launch {
                            val selecteData = FileSize.selectedBackRestore
                            Log.d(TAG, "onStartCommand: ${selecteData}")
                            selecteData.forEach { catType ->
                                Log.d(TAG, "onStartCommand: $catType")
                                if (type == bkpString) {
                                    Log.d(TAG, "onStartCommand: BackUp Inner")
                                    val itemList =
                                        repository.getBRItems(catType) as ArrayList<ItemModel>
                                    itemList.forEach {
                                        itemModelList.add(it)
                                    }

                                } else {
                                    Log.d(TAG, "onStartCommand: Restore Inner")
                                    val itemList =
                                        repository.getRBItems(catType) as ArrayList<ItemModel>
                                    itemList.forEach {
                                        itemModelList.add(it)
                                    }
                                }

                            }
                            Log.d(TAG, "onStartCommand: Button Type -----> $type")
                            Log.d(TAG, "onStartCommand: Total List Received ${itemModelList.toString()}")
                            if (itemModelList.isEmpty()) {
                                Log.d(TAG, "onStartCommand: No Items Found")
                                stopServiceMethod(type.toString() + " Completed!")
                            } else {
                                isInterrupted = false
                                createNotification(
                                    FileSize.BR_CHANNEl_ID,
                                    FileSize.BR_CHANNEL_NAME,
                                    FileSize.BR_NOTIFY_ID,
                                    FileSize.ACTION_SETTINGS_STOP_FOREGROUND_SERVICE
                                )
                                var dummyMsg = " Completed!"
                                for ((index, source) in itemModelList.withIndex()) {
                                    if (!isInterrupted!!) {
                                        try {
                                            Log.d(TAG, "onStartCommand: ${source.folderId}")
                                            val folderObj = repository.getFolderObjWithFolderID(source.folderId)
                                            Log.d(TAG, "onStartCommand: ${folderObj.toString()}")
                                            if (type == bkpString) {
                                                try {
                                                    Log.d(TAG, "onStartCommand: BackUp Process")
                                                    Log.d(TAG, "onStartCommand: ${source.itemCatType}/${source.folderId}")
                                                    val newFolderServId = getFolderBasedOnCategory(source.itemCatType,source.folderId)
                                                    source.folderId = newFolderServId
                                                    Log.d(TAG, "onStartCommand: Final Channel ${source.folderId}")
                                                    uploadLargeFiles(source, index + 1, folderObj)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    dummyMsg = " Cancelled!"
                                                    Log.d(TAG, "onStartCommand: Exception Occured-----> ${e.message}")
                                                }
                                            } else {
                                                var searchFile: java.io.File? = null
                                                try {
                                                    Log.d(TAG, "onStartCommand: Restore Process")
                                                    Log.d(TAG, "onStartCommand: ${folderObj.toString()}")
                                                    downloadDBFiles(source, index + 1)

                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    if (e.message.toString().contains("No such file or directory")) {
                                                        Log.d(TAG, "onStartCommand: No Local File available")
                                                    }
                                                    Log.d(TAG, "onStartCommand: Excep ${e.message}")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            dummyMsg = " Cancelled!"
                                            Log.d(TAG, "onStartCommand New Exception: ${e.message}")
                                        }
                                    } else break
                                }
                                stopServiceMethod(type.toString() + dummyMsg)
                                cancelProcessJob?.join()
                            }
                        }
                    }
                    FileSize.ACTION_SETTINGS_STOP_FOREGROUND_SERVICE -> {
                        Log.d(TAG, "onStartCommand: Stopped Foreground Service")
                        if (isInterrupted==true){

                        }
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

    fun stopServiceMethod(message: String) {
        Log.d(TAG, "stopServiceMethod: Called")
        settingsCallbackListner?.fileServerDealing(0, message, "")
        val clickIntent = Intent(this@DriveService, DriveService::class.java)
        clickIntent.action = FileSize.ACTION_SETTINGS_STOP_FOREGROUND_SERVICE
        clickIntent.putExtra(this@DriveService.resources.getString(R.string.key), message)
        startService(clickIntent)
    }

    inner class LocalBinder : Binder() {
        fun getService(): DriveService = this@DriveService

        fun settingsBRCallback(callback: SettingsCalBack) {
            Log.d(TAG, "SettingsCallback : Called")
            settingsCallbackListner = callback
        }

        fun startBackUpService(btnType: String) {
            Log.d(TAG, "startBackUpService: $btnType")
            val clickIntent = Intent(this@DriveService, DriveService::class.java)
            clickIntent.action = FileSize.ACTION_SETTINGS_START_FOREGROUND_SERVICE
            clickIntent.putExtra(this@DriveService.resources.getString(R.string.key), btnType)
            startService(clickIntent)
        }

        fun stopSettingsService(cancelMsg: String) {
            stopServiceMethod(cancelMsg)
        }

        fun getDriveStorage() {
            getTotalDriveStorages()
        }
    }

    fun getDriveFiles() {
        getDriveService()?.let { gdService ->
            val pagetoken: String? = null
            do {
                val result = gdService.files().list().apply {
                    spaces = "appDataFolder"
                    fields =
                        "nextPageToken, files(id,name,mimeType,quotaBytesUsed,createdTime,thumbnailLink,modifiedTime,modifiedByMeTime)"
                    var mimeType =
                        this@DriveService.resources.getString(R.string.file_mime_type)
//                    q = "mimeType='$mimeType'"
                    pageToken = this.pageToken
                }.execute()

                result?.let { res ->
                    res.files.forEach { file ->
                        Log.d(
                            "FILE",
                            " ${file.name} ${file.id} ${file.mimeType} ${file.thumbnailLink} ${
                                FileSize.bytesToHuman(
                                    file.quotaBytesUsed
                                )
                            }"
                        )
//                        Log.d(TAG, "getDriveFiles: $${file.createdTime} ${file.modifiedTime} ${file.modifiedByMeTime}")
//                        deleteData(file.id)
                    }
                }

            } while (pagetoken != null)
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
            val mimeType = "file/*"
            // File's new content.
            val file = File(this.resources.getString(R.string.db_path) + fileName)
            val newMetadata = com.google.api.services.drive.model.File()
            newMetadata.name = file.name

            // Convert content to an AbstractInputStreamContent instance.
            val contentStream = ByteArrayContent.fromString(mimeType, file.toString())
            val mediaContentNew =
                InputStreamContent("file/*", BufferedInputStream(FileInputStream(file)))
            // Send the request to the API.
            val fileRes = service.files().update(fileId, newMetadata, mediaContentNew)
                .setFields("id, name, appProperties,quotaBytesUsed").execute()
            fileRes?.let {
                Log.d(
                    TAG,
                    "UpdateDbFiles: Successfully ${fileRes.name} ${fileRes.id} ${fileRes.quotaBytesUsed}"
                )
            }
        } catch (e: IOException) {
            println("An error occurred: $e")
            Log.d(TAG, "UpdateDbFile Error --> : ${e.message}")
        }
    }

    fun downloadDBFiles(itemModel: ItemModel, itemNo: Int) {

        val out: OutputStream = FileOutputStream(itemModel.itemOriPath)
        val request: Drive.Files.Get? = getDriveService()?.files()?.get(itemModel.serverId)
        request?.let {
            val uploader: MediaHttpDownloader = it.mediaHttpDownloader
            uploader.isDirectDownloadEnabled = false
            uploader.chunkSize = 10 * MediaHttpUploader.MINIMUM_CHUNK_SIZE
            val downloadListner = CustomProgressListener(
                itemModel,
                itemNo,
                FileSize.bytesToHuman(itemModel.itemSize.toLong()).toString(),
                itemModel.itemOriPath.toString()
            )
            it.mediaHttpDownloader?.progressListener = downloadListner
            it.executeMediaAndDownloadTo(out)
        }
    }

    suspend fun uploadLargeFiles(itemModel: ItemModel, itemNo: Int, folderObj: FolderTable) {
        val fileMetadata = File()
        fileMetadata.name = itemModel.itemName
//        val catModel = repository.getDbServerFolderId(itemModel.itemCatType)
        val defLocation = this.resources.getString(R.string.db_folder_path)
        val folderNameCreation = "app_" + folderObj.folderCatType
        val sourceLoc = "$defLocation/$folderNameCreation/${folderObj.folderName}/${itemModel.itemName}"
        fileMetadata.parents = Collections.singletonList(itemModel.folderId)
        fileMetadata.mimeType = FileSize.getMimeType(sourceLoc)

        val file: java.io.File = File(sourceLoc)
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
            uploader.chunkSize = 8 * MediaHttpUploader.MINIMUM_CHUNK_SIZE
            val listner: CustomUploadProgressListener =
                CustomUploadProgressListener(itemModel, itemNo, fileSize!!)
            it.mediaHttpUploader?.progressListener = listner
            Log.d(TAG, "uploadLargeFiles: Process Two")
            it.execute()
        }

        responsee?.let {
            Log.d(TAG, "uploadLargeFiles: ServerFile ID->  ${it.id}")
            val res = repository.updateItemServerId(it.id, itemModel.itemId)
            Log.d(TAG, "uploadLargeFiles: $res")
            if ((itemModelList.size) == itemNo) {
                var msg = "Back-up Completed!"
                if (isInterrupted == true) msg = "Back-up Cancelled"
                stopServiceMethod(msg)
            }
        }
    }

    inner class CustomProgressListener(
        var itemModel: ItemModel,
        var itemNo: Int,
        var itemSize: String,
        var downloadPath: String
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
                            "$itemNo/$totalItems"
                        )
                        updateNotification(
                            respo.toInt(),
                            "$mbCount/$itemSize",
                            "$itemNo/$totalItems"
                        )
                        FileSize.backUpRestoreEnabled = true
                    } else {
                        val deleteFile = File(downloadPath)
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
                        isInterrupted = true
                        throw GoogleDriveException("Upload canceled")
                    }

                }
                UploadState.MEDIA_COMPLETE -> {
                    Log.d(TAG, "progressChanged: Upload is complete!")
                    Log.d(TAG, "progressChanged: TotalList ${itemModelList.size}")
                    Log.d(TAG, "progressChanged: ItemNo ${itemNo}")

                }
                UploadState.NOT_STARTED -> Log.d(TAG, "progressChanged: Upload Still Not Started")
            }
        }
    }

    fun getTotalDriveStorages() {
        val about: About? = getDriveService()?.let {
            gDriveService=it
            it.about().get().setFields("user, storageQuota").execute()
        }
        about?.let {
            val usedStorage = FileSize.bytesToHuman(about.storageQuota.usage)
            val totalStorage = FileSize.bytesToHuman(about.storageQuota.limit)
            settingsCallbackListner?.storageQuote(usedStorage.toString(), totalStorage.toString())
            Log.d("MainActivity", "getTotalDriveStorages: ${usedStorage.toString()}, ${totalStorage.toString()}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
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
        notificationLayout = RemoteViews(packageName, R.layout.collapsed_settings_notification)
        notificationLayout.setProgressBar(R.id.progressFile, 0, 0, true)
//        val notificationLayoutExpanded = RemoteViews(packageName, R.layout.notification_large)

        val clickIntent = Intent(this, SettingsActivity::class.java)
//        clickIntent.action = cancelAction
        clickIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        val filePendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationLayout.setOnClickPendingIntent(R.id.openSetNot, filePendingIntent)

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

    // .setQ("'${folderId}' in parents and mimeType='application/vnd.google-apps.folder'")
    fun getFilesUnderParticularFolder() {
        val folderId = "1Hy6XCqJKvcjDcClRKO5XErK5_tfWaQ7a_ORwzyasGctEjaIl2w"
        Log.d(TAG, "getFilesUnderParticularFolder: $folderId")
        val files: FileList? = getDriveService()?.let {
            it.files().list()
                .setSpaces("appDataFolder")
                .setQ("'${folderId}' in parents")
                .setFields("nextPageToken, files(id, name,quotaBytesUsed,permissions)")
                .execute()
        }
        files?.let {
            Log.d(TAG, "Db Files Under Folder in Server : Total List  ${it.files.size}")
            for (file in it.files) {
                Log.d(
                    TAG,
                    "getFilesUnderParticularFolder: ${file.name} ${file.quotaBytesUsed} ${file.permissions}"
                )
            }
        }
    }

    suspend fun getFolderBasedOnCategory(catId: String,folderId:String): String {
        var newFoldServId = ""
        Log.d(TAG, "getFolderBasedOnCategory: $folderId/$catId")
        val res: Deferred<FolderTable> = backgroundScope.async(Dispatchers.IO) {
            repository.getFolderObjBasedOnCatAndFolderID(catId,folderId)
        }
        val folderTable = res.await()
        Log.d(TAG, "getFolderBasedOnCategory: ${res.toString()}")
        val opRes = backgroundScope.launch(Dispatchers.IO) {
            if (!folderTable.folderTrash) {
                Log.d(TAG, "getFolderBasedOnCategory: Folder is Not Trash")
                if (folderTable.folderServerId.isEmpty()) {
                    Log.d(TAG, "getFolderBasedOnCategory: Folder Table Server Id is Empty")
                    newFoldServId = createFolderAndReturnServId(folderTable.folderName, repository.getCatServerId(folderTable.folderCatType))
                    Log.d(TAG, "getFolderBasedOnCategory: ${folderTable.folderCatType}/${newFoldServId}")
                    repository.updateFolderServIdBasedOnFolderId(folderTable.folderId.toString(), newFoldServId)
                } else {
                    Log.d(TAG, "getFolderBasedOnCategory: Folder Table Server Id is Not Empty")
                    updateFolderName(folderTable)
                    newFoldServId = folderTable.folderServerId
                }
            } else {
                Log.d(TAG, "getFolderBasedOnCategory: Folder is in Trash")
                newFoldServId = "Trash"
            }
        }
        opRes.join()
        Log.d(TAG, "getFolderBasedOnCategory: $newFoldServId")
        return newFoldServId
    }

    fun updateFolderName(folerTable: FolderTable) {
        backgroundScope.launch(Dispatchers.IO) {
            try {
                val mimeType = this@DriveService.resources.getString(R.string.folder_mime_type)
                // File's new content.
                val newMetadata = com.google.api.services.drive.model.File()
                newMetadata.name = folerTable.folderName

                // Send the request to the API.
                val fileRes = getDriveService()!!.files().update(folerTable.folderServerId, newMetadata)
                    .setFields("id, name, appProperties,quotaBytesUsed").execute()
                fileRes?.let {
                    Log.d(TAG, "updateFolderName: Successfully ${fileRes.name} ${fileRes.id} ${fileRes.quotaBytesUsed}")
                }
            } catch (e: IOException) {
                println("An error occurred: $e")
                Log.d(TAG, "updateFolderName Error --> : ${e.message}")
            }
        }
    }

    fun deleteData(fileId: String) {
        val files = getDriveService()?.let {
            it.files().delete(fileId).execute()
        }
    }

    fun createFolderAndReturnServId(folderName: String, parentId: String): String {
        val fileMetadata = File()
        val folderMime = this.resources.getString(R.string.folder_mime_type)
        fileMetadata.name = folderName
        fileMetadata.parents = Collections.singletonList(parentId)
        fileMetadata.mimeType = folderMime

        val file = getDriveService()?.let {
            it.files().create(fileMetadata)
                .setFields("id,name")
                .execute()
        }
        return file?.id.toString()
    }

    interface SettingsCalBack {
        fun fileServerDealing(progress: Int, mbCount: String, totalItems: String)
        fun storageQuote(totalStorage: String, usedStorage: String)
    }

}