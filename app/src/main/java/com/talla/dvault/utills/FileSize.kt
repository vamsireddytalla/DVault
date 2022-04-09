package com.talla.dvault.utills

import android.app.Activity
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import java.text.DecimalFormat
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.view.View
import com.talla.dvault.database.entities.ItemModel
import java.io.File
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.database.getStringOrNull
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.SourcesModel
import com.talla.dvault.models.CustomItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.lang.Exception

object FileSize {
    fun floatForm(d: Double): String {
        return DecimalFormat("#.##").format(d)
    }

    var OnLongItemClick:Boolean=false
    var customLongItemClick:Boolean=false
    var SelectAll:Boolean=false
    var backUpRestoreEnabled:Boolean=false
    var selectedUnlockItems: MutableSet<ItemModel> = mutableSetOf()
    var selectedBackRestore: MutableSet<String> = mutableSetOf()
    var selectedCustomItems: MutableSet<CustomItemModel> = mutableSetOf()
    const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
    const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"
    const val ACTION_UNLOCK_START_FOREGROUND_SERVICE = "ACTION_UNLOCK_START_FOREGROUND_SERVICE"
    const val ACTION_UNLOCK_STOP_FOREGROUND_SERVICE = "ACTION_UNLOCK_STOP_FOREGROUND_SERVICE"
    const val ACTION_SETTINGS_START_FOREGROUND_SERVICE = "ACTION_SETTINGS_START_FOREGROUND_SERVICE"
    const val ACTION_SETTINGS_STOP_FOREGROUND_SERVICE = "ACTION_SETTINGS_STOP_FOREGROUND_SERVICE"
    const val FILE_ADD_CHANNEl_ID = "101"
    const val FILE_ADD_CHANNEL_NAME = "FILE_ADD_PROCESSING_NOTIFICATION"
    const val FILE_UNLOCK_CHANNEl_ID = "102"
    const val FILE_UNLOCK_CHANNEL_NAME = "FILE_UNLOCK_PROCESSING_NOTIFICATION"
    const val BR_CHANNEl_ID = "103"
    const val BR_CHANNEL_NAME = "BR_PROCESSING_NOTIFICATION"
    const val FILE_NOTIFY_ID = 100
    const val UNLOCK_FILE_NOTIFY_ID = 99
    const val  BR_NOTIFY_ID= 98
    var FILE_COPYING:Boolean = false
    var UNLOCK_FILE_COPYING:Boolean = false
    var settingsBRSelected:String=""


    fun bytesToHuman(size: Long): String? {
        val Kb = (1 * 1024).toLong()
        val Mb = Kb * 1024
        val Gb = Mb * 1024
        val Tb = Gb * 1024
        val Pb = Tb * 1024
        val Eb = Pb * 1024
        if (size < Kb) return floatForm(size.toDouble()) + " byte"
        if (size >= Kb && size < Mb) return floatForm(size.toDouble() / Kb) + " Kb"
        if (size >= Mb && size < Gb) return floatForm(size.toDouble() / Mb) + " Mb"
        if (size >= Gb && size < Tb) return floatForm(size.toDouble() / Gb) + " Gb"
        if (size >= Tb && size < Pb) return floatForm(size.toDouble() / Tb) + " Tb"
        if (size >= Pb && size < Eb) return floatForm(size.toDouble() / Pb) + " Pb"
        return if (size >= Eb) floatForm(size.toDouble() / Eb) + " Eb" else "???"
    }

    // Get internal (data partition) free space
    // This will match what's shown in System Settings > Storage for
    // Internal Space, when you subtract Total - Used
    fun getFreeInternalMemory(): Long {
        return getFreeMemory(Environment.getDataDirectory())
    }

    // Get external (SDCARD) free space
    fun getFreeExternalMemory(): Long {
        return getFreeMemory(Environment.getExternalStorageDirectory())
    }

    // Get Android OS (system partition) free space
    fun getFreeSystemMemory(): Long {
        return getFreeMemory(Environment.getRootDirectory())
    }


    // Get free space for provided path
    // Note that this will throw IllegalArgumentException for invalid paths
    fun getFreeMemory(path: File): Long {
        val stats = StatFs(path.getAbsolutePath())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stats.availableBlocksLong * stats.blockSizeLong
        } else {
            return 0
        }
    }

    fun getMimeType(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }

    fun showSnackBar(message: String,view:View) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    fun checkIsAnyProcessGoing():String
    {
        var returnMsg=""
        if (FileSize.backUpRestoreEnabled){
            returnMsg="Backup or Restore is in Processing.Please Wait!"
        }
        else if (FileSize.FILE_COPYING){
            returnMsg="File Copying is under Processing.Please Wait!"
        }else if (FileSize.UNLOCK_FILE_COPYING){
            returnMsg="Some Files are Unlocking.Please Wait!"
        }
        return returnMsg
    }

    fun checkVersion30() : Boolean{
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun imageConvertData(uri: Uri,context:Activity,folderTable:FolderTable):SourcesModel? {
      var sourceModel:SourcesModel?=null
        withContext(Dispatchers.IO) {

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.ALBUM
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val size = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mime_type = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val albumColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ALBUM)


                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val sizee = cursor.getInt(size)
                    val mime = cursor.getString(mime_type)
                    val albumColumn = cursor.getStringOrNull(albumColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    sourceModel=SourcesModel(contentUri.toString(),displayName,sizee.toLong(),mime, folderTable)
                }

            }
        }
        return sourceModel
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun videoConvertData(uri: Uri,context:Activity,folderTable:FolderTable):SourcesModel? {
        var sourceModel:SourcesModel?=null
        withContext(Dispatchers.IO) {

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.ALBUM
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val size = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mime_type = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val albumColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)


                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val sizee = cursor.getInt(size)
                    val mime = cursor.getString(mime_type)
                    val albumColumn = cursor.getStringOrNull(albumColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    sourceModel=SourcesModel(contentUri.toString(),displayName,sizee.toLong(),mime, folderTable)
                }

            }
        }
        return sourceModel
    }

}