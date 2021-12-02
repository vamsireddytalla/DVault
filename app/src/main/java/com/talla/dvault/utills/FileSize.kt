package com.talla.dvault.utills

import android.os.Build
import java.text.DecimalFormat
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.view.View
import com.talla.dvault.database.entities.ItemModel
import java.io.File
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar

object FileSize {
    fun floatForm(d: Double): String {
        return DecimalFormat("#.##").format(d)
    }

    var OnLongItemClick:Boolean=false
    var SelectAll:Boolean=false
    var backUpRestoreEnabled:Boolean=false
    var selectedUnlockItems: MutableSet<ItemModel> = mutableSetOf()
    var selectedBackRestore: MutableSet<String> = mutableSetOf()
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

}