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
import android.os.Binder
import androidx.core.net.toFile
import java.lang.Exception


private const val TAG = "FileCopyService"
private const val CHANNEl_ID = "101"
private const val CHANNEL_NAME = "FILE_PROCESSING_NOTIFICATION"
const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"
const val FILE_NOTIFY_ID = 100

@AndroidEntryPoint
class FileCopyService : Service() {

    private lateinit var notificationLayout: RemoteViews
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification
    private lateinit var builder: NotificationCompat.Builder
    private var job: Job? = null
    private var totalItemsDone:Int=0
    private var sourceModelList: List<SourcesModel>? = null
    private var isInterrupted: Boolean? = false
    private var progress:Int=0
    private var mbCopied:String=""
    private var totalCount:String="0/1"
    // Binder given to clients
    private val binder: IBinder = LocalBinder()

    @Inject
    lateinit var repository: VaultRepository


    override fun onCreate() {
        super.onCreate()
        isInterrupted = false
        Log.d(TAG, "onCreate: From FileCopyService Called")
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): FileCopyService = this@FileCopyService
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: From FileCopyService Called")

        if (intent != null) {
            val action = intent.action
            when (action) {
                ACTION_START_FOREGROUND_SERVICE -> {
                    createNotification()
                    if (intent.extras != null) {
                        sourceModelList = intent.getSerializableExtra(this.resources.getString(R.string.fileCopy)) as List<SourcesModel>
                        job = GlobalScope.launch(Dispatchers.IO) {
                            sourceModelList?.forEach { sourceModel ->
                                var itemModel = fromUriGetRealPath(sourceModel)
                                repository.insertSingleItem(itemModel)
                            }
                            stopForegroundService()
                        }
                    }
                }
                ACTION_STOP_FOREGROUND_SERVICE -> {
                    Log.d(TAG, "onStartCommand: Stopped Foreground Service")
                    job?.let {
                        it.cancel()
                        isInterrupted = true
                    }
                    stopForegroundService()
                }
            }
        }


        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    @SuppressLint("RemoteViewLayout")
    private fun createNotification() {
        Log.d(TAG, "createNotification: Called")
        var channelId: String? = null
        //createNotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel(CHANNEl_ID, CHANNEL_NAME)
        }
        // Create notification default intent.
        val intent = Intent()
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)


        // Get the layouts to use in the custom notification
        notificationLayout = RemoteViews(packageName, R.layout.collapsed_item_progress)
        notificationLayout.setProgressBar(R.id.progressFile, 0, 0, true)
//        val notificationLayoutExpanded = RemoteViews(packageName, R.layout.notification_large)

        var clickIntent = Intent(this, FileCopyService::class.java)
        clickIntent.action = ACTION_STOP_FOREGROUND_SERVICE;
        var filePendingIntent =
            PendingIntent.getService(this, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationLayout.setOnClickPendingIntent(R.id.cancelFileProcess, filePendingIntent)

        // Create notification builder.
        builder = NotificationCompat.Builder(this, channelId.toString())
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
        startForeground(FILE_NOTIFY_ID, notification)
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
    private fun updateNotification(progress: Int, mbCopied: String,totalCount:String) {
        this.progress=progress
        this.mbCopied=mbCopied
        this.totalCount=totalCount
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

        // update the notification
        if (api < VERSION_CODES.HONEYCOMB) {
            notificationManager.notify(FILE_NOTIFY_ID, notification)
        } else if (api >= VERSION_CODES.HONEYCOMB) {
            notificationManager.notify(FILE_NOTIFY_ID, builder.build())
        }
    }

    //copying file from one place to other
    private fun copyFile(
        oldFileLoc: String,
        fileName: String,
        catFolderName: String,
        contentUri: Uri
    ): String {

        val newdir: File = this.getDir(catFolderName, Context.MODE_PRIVATE) //Don't do
        Log.d(TAG, newdir.toString())
        if (!newdir.exists()) {
            newdir.mkdirs()
            Log.d(TAG, "createFolder: Creating")
        }
        val to = File("$newdir/$fileName")
        val from = File(oldFileLoc)
        Log.d(TAG, "Old File Location $oldFileLoc")
        try {

            val inStream: InputStream = FileInputStream(from)
            val outStream: OutputStream = FileOutputStream(to)

            val lenghtOfFile: Int = inStream.available()
            val buf = ByteArray(1024*1024
            )
            var len: Int
            var total: Long = 0
            var fileTotalSize = getFileSize(from)
            while (inStream.read(buf).also { len = it } != -1) {
                total += len.toLong()
                var bytes: Int = (total * 100 / lenghtOfFile).toInt()
                outStream.write(buf, 0, len)
                var res = FileSize.bytesToHuman(total.toLong())
                updateNotification(bytes, res + " / " + fileTotalSize.toString(),totalItemsDone.toString()+"/"+sourceModelList?.size)
                if (isInterrupted!!) {
                    break
                }
                outStream.flush()
            }
            if (!isInterrupted!!) {
                Log.d(TAG, "copyFile: Not Interupted")
                var res = delete(this, from)
                Log.d(TAG, "Delete File $res")
                if (!res) {
                    var newDel = from.delete()
                    Log.d(TAG, "New Delete $newDel")
                }
//                if (res) {
//                    this.contentResolver.notifyChange(contentUri, null)
//                }
            }
            totalItemsDone+=1
            inStream.close()
            outStream.flush()
            outStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "copyFile: ${e.message}")
        }

        return to.toString()
    }


    fun delete(context: Context, file: File): Boolean {
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


    fun fromUriGetRealPath(sourcesModel: SourcesModel): ItemModel {
        val fileRealPath: String? = RealPathUtill.getRealPath(this, sourcesModel.source.toUri())
        var filesize = getFileSize(File(fileRealPath))
        var file = File(fileRealPath)
        var newFilePath: String = copyFile(
            fileRealPath.toString(),
            file.name,
            sourcesModel.catType,
            sourcesModel.source.toUri()
        )
        Log.d(TAG, "File size : ${filesize}")
        Log.d(TAG, "File Real Path : ${fileRealPath}")
        Log.d(TAG, "File New Path : ${newFilePath}")

        return ItemModel(
            itemName = file.name,
            itemSize = filesize.toString(),
            itemCreatedAt = DateUtills.getSystemTime(this).toString(),
            itemMimeType = sourcesModel.catType,
            itemOriPath = fileRealPath.toString(),
            folderId = sourcesModel.folderID.toString(),
            itemCurrentPath = newFilePath.toString()
        )
    }


    fun getFileSize(file: File): String? {

        val format: DecimalFormat = DecimalFormat("#.##")
        val MiB = (1024 * 1024).toLong()
        val GiB = (1024 * 1024 * 1024).toLong()
        val KiB: Long = 1024
        Log.d(TAG, "getFileSize: $file")
        require(file.isFile) { "Expected a file" }
        val length = file.length().toDouble()
        if (length > GiB) {
            return format.format(length / GiB).toString() + " Gb"
        } else if (length > MiB) {
            return format.format(length / MiB).toString() + " Mb"
        }
        return if (length > KiB) {
            format.format(length / KiB).toString() + " Kb"
        } else format.format(length).toString() + " B"
    }


    private fun stopForegroundService() {
        Log.d(TAG, "stopForegroundService: Called")
        // Stop foreground service and remove the notification.
        stopForeground(true)
        // Stop the foreground service.
        stopSelf()
    }


    interface fileCopyListner{
        fun fileListner(progress:Int,mbCount:String,totalItems:String)
    }

}