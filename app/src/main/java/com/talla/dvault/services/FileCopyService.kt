package com.talla.dvault.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.talla.dvault.R
import android.app.PendingIntent
import android.net.Uri
import android.os.Build.VERSION_CODES
import androidx.core.net.toUri
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.database.entities.SourcesModel
import com.talla.dvault.repositories.VaultRepository
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.utills.FileSize
import com.talla.dvault.utills.RealPathUtill
import com.talla.dvault.viewmodels.ItemViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.*
import java.text.DecimalFormat
import javax.inject.Inject
import javax.xml.transform.Source
import android.os.Environment

import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.provider.MediaStore

import android.content.ContentResolver
import android.content.ServiceConnection
import android.os.Binder
import androidx.core.net.toFile
import androidx.lifecycle.MutableLiveData
import com.talla.dvault.activities.ItemsActivity
import java.lang.Exception
import android.app.NotificationManager

import android.app.NotificationChannel
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.talla.dvault.database.entities.DeleteFilesTable
import com.talla.dvault.database.entities.FolderTable
import kotlin.math.log


private const val TAG = "FileCopyService"

@AndroidEntryPoint
class FileCopyService : Service() {

    private lateinit var notificationLayout: RemoteViews
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification
    private lateinit var builder: NotificationCompat.Builder
    private var lockJob: Job? = null
    private var unlockJob: Job? = null
    private var sourceModelList: ArrayList<SourcesModel>? = null
    private var itemModelList: MutableSet<ItemModel>? = null
    private var isInterrupted: Boolean? = false
    private var UNLOCK_INTERRUPT: Boolean? = false

    val randomNumberLiveData: MutableLiveData<Int> = MutableLiveData()
    private val binder: IBinder = LocalBinder()
    private var fileCopyCallBack: FileCopyCallback? = null

    @Inject
    lateinit var repository: VaultRepository

    override fun onCreate() {
        super.onCreate()
        isInterrupted = false
        randomNumberLiveData.value = 0
        startServiceOreoCondition()
        Log.d(TAG, "onCreate: From FileCopyService Called")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: called")
        if (intent != null) {
            val action = intent.action
            when (action) {
                FileSize.ACTION_START_FOREGROUND_SERVICE -> {
                    if (intent.extras != null) {
                        Log.d(TAG, "onStartCommand: ACTION_START_FOREGROUND_SERVICE")
                        sourceModelList?.clear()
                        sourceModelList =
                            intent.getSerializableExtra(this.resources.getString(R.string.fileCopy)) as ArrayList<SourcesModel>
                        Log.d(TAG, "Hash Code -> : ${sourceModelList.hashCode()}")
                        isInterrupted = false
                        createNotification(
                            FileSize.FILE_ADD_CHANNEl_ID,
                            FileSize.FILE_ADD_CHANNEL_NAME,
                            FileSize.FILE_NOTIFY_ID,
                            FileSize.ACTION_STOP_FOREGROUND_SERVICE
                        )
                        lockJob = GlobalScope.launch(Dispatchers.IO) {
                            sourceModelList?.let { sourceModel ->
                                for ((index, source) in sourceModel.withIndex()) {
                                    try {
                                        val itemModel = fromUriGetRealPath(source, index)
                                        if (!isInterrupted!!) {
                                            repository.insertSingleItem(itemModel)
                                            if (FileSize.checkVersion30()) {
                                                repository.insertDeleteFileTable(
                                                    DeleteFilesTable(source.sourceFilePath,source.folderTable.folderCatType)
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.d(TAG, "onStartCommand Exception: ${e.message}")
                                    }
                                }
                                FileSize.selectedCustomItems.clear()
                            }
                            fileCopyCallBack?.fileCopyCallBack(0, "Completed", "")
                            FileSize.FILE_COPYING = false
                            stopForeground(false)
                            notificationManager.cancel(FileSize.FILE_NOTIFY_ID)
                        }
                    }
                }
                FileSize.ACTION_STOP_FOREGROUND_SERVICE -> {
                    Log.d(TAG, "onStartCommand: ACTION_STOP_FOREGROUND_SERVICE")
                    lockJob?.let {
                        isInterrupted = true
                    }
                    fileCopyCallBack?.fileCopyCallBack(0, "Cancelled", "0/0")
                    FileSize.FILE_COPYING = false
                    this.stopForeground(false)
                    notificationManager.cancel(FileSize.FILE_NOTIFY_ID)
                }
                FileSize.ACTION_UNLOCK_START_FOREGROUND_SERVICE -> {
                    Log.d(TAG, "onStartCommand: ACTION_UNLOCK_START_FOREGROUND_SERVICE")
                    if (intent.extras != null) {
                        itemModelList?.clear()
                        itemModelList =
                            intent.getSerializableExtra(this.resources.getString(R.string.unlockedList)) as MutableSet<ItemModel>
                        Log.d(TAG, "Hash Code -> : ${itemModelList.hashCode()}")
                        UNLOCK_INTERRUPT = false
                        createNotification(
                            FileSize.FILE_UNLOCK_CHANNEl_ID,
                            FileSize.FILE_UNLOCK_CHANNEL_NAME,
                            FileSize.UNLOCK_FILE_NOTIFY_ID,
                            FileSize.ACTION_UNLOCK_STOP_FOREGROUND_SERVICE
                        )
                        unlockJob = GlobalScope.launch(Dispatchers.IO) {
                            itemModelList?.let { itemModel ->
                                for ((index, source) in itemModel.withIndex()) {
                                    try {
                                        Log.d(
                                            TAG,
                                            "onStartCommand: Action Unlock  ${itemModel.size}"
                                        )
                                        val folderObj =
                                            repository.getFolderObjWithFolderID(source.folderId)
                                        unlockingFile(folderObj, source.itemName, index)
                                        if (!UNLOCK_INTERRUPT!!) {
                                            if (source.serverId.isEmpty()) {
                                                repository.deleteItem(source.itemId)
                                            }
                                            Log.d(TAG, "onStartCommand: ${source.itemMimeType}")
                                        }
                                    } catch (e: Exception) {
                                        Log.d(TAG, "onStartCommand: ${e.message}")
                                    }
                                }
                            }
                            fileCopyCallBack?.fileUnlockingCallBack(0, "Completed", "")
                            FileSize.UNLOCK_FILE_COPYING = false
                            stopForeground(false)
                            notificationManager.cancel(FileSize.UNLOCK_FILE_NOTIFY_ID)
                        }
                    }
                }
                FileSize.ACTION_UNLOCK_STOP_FOREGROUND_SERVICE -> {
                    Log.d(TAG, "onStartCommand: ACTION_UNLOCK_STOP_FOREGROUND_SERVICE")
                    unlockJob?.let {
                        UNLOCK_INTERRUPT = true
                    }
                    fileCopyCallBack?.fileUnlockingCallBack(0, "Cancelled", "0/0")
                    FileSize.UNLOCK_FILE_COPYING = false
                    this.stopForeground(false)
                    notificationManager.cancel(FileSize.UNLOCK_FILE_NOTIFY_ID)
                }
            }
        }

        return START_STICKY
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d(TAG, "onRebind: Called")
    }

    override fun unbindService(conn: ServiceConnection) {
        super.unbindService(conn)
        Log.d(TAG, "unbindService: called")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: called")
        return super.onUnbind(intent)
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): FileCopyService = this@FileCopyService

        fun copyFileCallBack(callback: FileCopyCallback) {
            Log.d(TAG, "copyFileCallBack: Called")
            fileCopyCallBack = callback
        }


        fun startFileCopyingService(sourceItemList: List<SourcesModel>) {
            val clickIntent = Intent(this@FileCopyService, FileCopyService::class.java)
            clickIntent.action = FileSize.ACTION_START_FOREGROUND_SERVICE
            clickIntent.putExtra(getString(R.string.fileCopy), sourceItemList as Serializable)
            Log.d(TAG, "startFileCopyingService: ${sourceModelList?.size}")
            startService(clickIntent)
        }

        fun stopFileProcessing(flagType: String) {

            val unLockIntent = Intent(this@FileCopyService, FileCopyService::class.java)
            if (flagType == "Unlock") {
                unLockIntent.action = FileSize.ACTION_UNLOCK_STOP_FOREGROUND_SERVICE
            } else {
                unLockIntent.action = FileSize.ACTION_STOP_FOREGROUND_SERVICE
            }
            startService(unLockIntent)
        }

        fun unlockFilesService(itemModelList: MutableSet<ItemModel>) {
            val unLockIntent = Intent(this@FileCopyService, FileCopyService::class.java)
            unLockIntent.action = FileSize.ACTION_UNLOCK_START_FOREGROUND_SERVICE
            unLockIntent.putExtra(getString(R.string.unlockedList), itemModelList as Serializable)
            Log.d(TAG, "Start UnlockingFile Service : ${itemModelList.size}")
            startService(unLockIntent)
        }

    }

    fun stopServiceAndUnBind() {
        var clickIntent = Intent(this@FileCopyService, FileCopyService::class.java)
        clickIntent.action = FileSize.ACTION_STOP_FOREGROUND_SERVICE;
        startService(clickIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind: ")
        return binder
    }

    @SuppressLint("RemoteViewLayout")
    private fun createNotification(
        channelId: String,
        channelName: String,
        file_notify_id: Int,
        cancelAction: String
    ) {
        Log.d(TAG, "createNotification: Called")
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

        val clickIntent = Intent(this, FileCopyService::class.java)
        clickIntent.action = cancelAction
        val filePendingIntent =
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(chan)
        return channelId
    }

    // use this method to update the Notification's UI
    private fun updateNotification(
        progress: Int,
        mbCopied: String,
        totalCount: String,
        flag: String
    ) {
        val api = Build.VERSION.SDK_INT
        // update the icon
        notificationLayout.setProgressBar(R.id.progressFile, 100, progress, false)
        // update the title
        notificationLayout.setTextViewText(
            R.id.totalElapsed,
            mbCopied
        )
        notificationLayout.setTextViewText(
            R.id.totalCount,
            totalCount
        )
        randomNumberLiveData.postValue(progress)
        if (flag == "Unlock") {
            fileCopyCallBack?.fileUnlockingCallBack(progress, mbCopied, totalCount)
            notificationLayout.setTextViewText(
                R.id.addingVaultTitle,
                "Unlocking Files \n${this.resources.getString(R.string.file_sys_dst)}"
            )
            // update the notification
            if (api < VERSION_CODES.HONEYCOMB) {
                notificationManager.notify(FileSize.UNLOCK_FILE_NOTIFY_ID, notification)
            } else if (api >= VERSION_CODES.HONEYCOMB) {
                notificationManager.notify(FileSize.UNLOCK_FILE_NOTIFY_ID, builder.build())
            }
        } else {
            fileCopyCallBack?.fileCopyCallBack(progress, mbCopied, totalCount)
            notificationLayout.setTextViewText(R.id.addingVaultTitle, "Adding To D-Vault")
            // update the notification
            if (api < VERSION_CODES.HONEYCOMB) {
                notificationManager.notify(FileSize.FILE_NOTIFY_ID, notification)
            } else if (api >= VERSION_CODES.HONEYCOMB) {
                notificationManager.notify(FileSize.FILE_NOTIFY_ID, builder.build())
            }
        }


    }

    //Unlocking file from one place to other
    private fun unlockingFile(folderTable: FolderTable, itmName: String, itemNo: Int) {

        val defLocation = this.resources.getString(R.string.db_folder_path)
        val folderNameCreation = "app_" + folderTable.folderCatType
        val sourceLoc = "$defLocation/$folderNameCreation/${folderTable.folderName}/$itmName"
        val sourceLo = File(sourceLoc)
        var finalDst: String? = null
        try {

            val inStream: InputStream = FileInputStream(sourceLo)
            val dsinationLoc = this.resources.getString(R.string.file_sys_dst)
            val destLo = File(dsinationLoc)
            if (!destLo.exists()) {
                destLo.mkdirs()
            }
            finalDst = "$destLo/$itmName"
            val outStream: OutputStream = FileOutputStream(finalDst)

            val lenghtOfFile: Int = inStream.available()
            val buf = ByteArray(1024 * 1024)
            var len: Int
            var total: Long = 0
            val fileTotalSize = getFileSize(sourceLo.length())
            while (inStream.read(buf).also { len = it } != -1) {
                FileSize.UNLOCK_FILE_COPYING = true
                total += len.toLong()
                val bytes: Int = (total * 100 / lenghtOfFile).toInt()
                outStream.write(buf, 0, len)
                val res = FileSize.bytesToHuman(total.toLong())
                val totalItemsList = itemModelList?.size
                updateNotification(
                    bytes,
                    res + " / " + fileTotalSize.toString(),
                    itemNo.toString() + "/" + totalItemsList,
                    "Unlock"
                )
                if (UNLOCK_INTERRUPT!!) {
                    val fff = File(finalDst)
                    fff.delete()
                    break
                }
                outStream.flush()
            }
            if (!UNLOCK_INTERRUPT!!) {
                Log.d(TAG, "copyFile: Not Interupted")
                val res = delete(this, sourceLo)
                Log.d(TAG, "Delete File $res")
                if (!res) {
                    val newDel = sourceLo.delete()
                    Log.d(TAG, "New Delete $newDel")
                }
            }
            inStream.close()
            outStream.flush()
            outStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            val fff = File(finalDst)
            fff.delete()
            Log.d(TAG, "Unlock File Exception --> : ${e.message}")
        }

    }

    private fun delete(context: Context, file: File): Boolean {
        val where = MediaStore.MediaColumns.DATA + "=?"
        val selectionArgs = arrayOf(
            file.absolutePath
        )
        val contentResolver = context.contentResolver
        val filesUri = MediaStore.Files.getContentUri("external")
        contentResolver.delete(filesUri, where, selectionArgs)
        if (file.exists()) {
            contentResolver.delete(filesUri, where, selectionArgs)
        }
        return !file.exists()
    }

    private fun fromUriGetRealPath(sourcesModel: SourcesModel, itemNo: Int): ItemModel {

        Log.d(TAG, "File size : ${sourcesModel.fileSize}")
        Log.d(TAG, "File Name : ${sourcesModel.fileName}")
        Log.d(TAG, "File Real Path : ${sourcesModel.sourceFilePath}")
        val newFilePath = copyFile(sourcesModel, itemNo)
        Log.d(TAG, "File New Path : ${newFilePath}")

        return ItemModel(
            itemName = sourcesModel.fileName,
            itemSize = sourcesModel.fileSize.toString(),
            itemCreatedAt = System.currentTimeMillis().toString(),
            itemMimeType = FileSize.getMimeType(sourcesModel.sourceFilePath).toString(),
            folderId = sourcesModel.folderTable.folderId.toString(),
            itemCurrentPath = newFilePath,
            itemCatType = sourcesModel.folderTable.folderCatType
        )
    }

    //copying file from one place to other
    private fun copyFile(sourcesModel: SourcesModel, itemNo: Int): String {
        val newdir: File = this.getDir(sourcesModel.folderTable.folderCatType, Context.MODE_PRIVATE)
        val defLocation = this.resources.getString(R.string.db_folder_path)
        val loc = application.filesDir
        Log.d(TAG, "copyFile: Loc   $loc")
        val folderNameCreation = "app_" + sourcesModel.folderTable.folderCatType
        val sourceLoc = "$defLocation/$folderNameCreation/${sourcesModel.folderTable.folderName}"
        Log.d(TAG, "File Path " + sourceLoc.toString())
        if (!File(sourceLoc).exists()) {
            File(sourceLoc).mkdirs()
            Log.d(TAG, "createFolder: Creating")
        }
        val to = File("$sourceLoc/${sourcesModel.fileName}")
        val from = File(sourcesModel.sourceFilePath)
        Log.d(TAG, "Old File Location ${sourcesModel.sourceFilePath}")
        try {
            if (FileSize.checkVersion30()) {
                sdk30AboveCopyFile(sourcesModel, itemNo, to)
            } else {
                val inStream: InputStream = FileInputStream(from)
                val outStream: OutputStream = FileOutputStream(to)

                val lenghtOfFile: Int = inStream.available()
                val buf = ByteArray(1024 * 1024)
                var len: Int
                var total: Long = 0
                val fileTotalSize = getFileSize(sourcesModel.fileSize)
                Log.d(TAG, "copyFile: File Size testing $fileTotalSize")
                while (inStream.read(buf).also { len = it } != -1) {
                    FileSize.FILE_COPYING = true
                    total += len.toLong()
                    val bytes: Int = (total * 100 / lenghtOfFile).toInt()
                    outStream.write(buf, 0, len)
                    val res = FileSize.bytesToHuman(total.toLong())
                    val totalItemsList = sourceModelList?.size
                    updateNotification(
                        bytes,
                        res + " / " + fileTotalSize.toString(),
                        itemNo.toString() + "/" + totalItemsList,
                        "Copy"
                    )
                    if (isInterrupted!!) {
                        break
                    }
                    outStream.flush()
                }
                if (!isInterrupted!!) {
                    Log.d(TAG, "copyFile: Not Interupted")
                    val res = delete(this, from)
                    Log.d(TAG, "Delete File $res")
                    if (!res) {
                        val newDel = from.delete()
                        Log.d(TAG, "New Delete $newDel")
                    }
//                if (res) {
//                    this.contentResolver.notifyChange(contentUri, null)
//                }
                }
                inStream.close()
                outStream.flush()
                outStream.close()
            }


        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "copyFile: ${e.message}")
        }

        return to.toString()
    }

    private fun sdk30AboveCopyFile(sourcesModel: SourcesModel, itemNo: Int, to: File) {
        val resolver = applicationContext.contentResolver
        // "rw" for read-and-write;
        // "rwt" for truncating or overwriting existing file contents.
        val readOnlyMode = "rwt"
        resolver.openInputStream(sourcesModel.sourceFilePath.toUri()).use { inputStream ->
            // Perform operations on "stream".
            // Perform operations on "pfd".
            val file =
                File(filesDir.absoluteFile.path + File.separator + "${sourcesModel.fileName}")
            val optStream = FileOutputStream(to)
            val buffers = ByteArray(1024 * 1024)
            var len: Int
            var total: Long = 0
            val lenghtOfFile: Int = inputStream!!.available()
            val fileTotalSize = getFileSize(sourcesModel.fileSize)
            val totalItemsList = sourceModelList?.size
            while (inputStream.read(buffers).also { len = it } != -1) {
                FileSize.FILE_COPYING = true
                val bytes: Int = (total * 100 / lenghtOfFile).toInt()
                total += len.toLong()
                optStream.write(buffers, 0, len)
                val res = FileSize.bytesToHuman(total.toLong())
                updateNotification(
                    bytes,
                    res + " / " + fileTotalSize.toString(),
                    itemNo.toString() + "/" + totalItemsList,
                    "Copy"
                )
                if (isInterrupted!!) {
                    break
                }
            }
            inputStream.close()
            optStream.flush()
            optStream.close()
        }
    }

    private fun getFileSize(fileLength: Long): String? {

        val format: DecimalFormat = DecimalFormat("#.##")
        val MiB = (1024 * 1024).toLong()
        val GiB = (1024 * 1024 * 1024).toLong()
        val KiB: Long = 1024
        Log.d(TAG, "getFileSize: $fileLength")
        //require(file.isFile) { "Expected a file" }
        if (fileLength > GiB) {
            return format.format(fileLength / GiB).toString() + " Gb"
        } else if (fileLength > MiB) {
            return format.format(fileLength / MiB).toString() + " Mb"
        }
        return if (fileLength > KiB) {
            format.format(fileLength / KiB).toString() + " Kb"
        } else format.format(fileLength).toString() + " B"
    }

    private fun stopForegroundService() {
        Log.d(TAG, "stopForegroundService: Called")
        // Stop foreground service and remove the notification.
        stopForeground(true)
        // Stop the foreground service.
        stopSelf()
    }

    interface FileCopyCallback {
        fun fileCopyCallBack(progress: Int, mbCount: String, totalItems: String)

        fun fileUnlockingCallBack(progress: Int, mbCount: String, totalItems: String)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: called")
    }

    private fun startServiceOreoCondition() {
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "888"
            val CHANNEL_NAME = "DEFAULT_SERVICE"
            val channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.drawable.notification_icon)
                .setPriority(PRIORITY_HIGH)
                .setNotificationSilent()
                .build()
            startForeground(888, notification)
        }
    }


}