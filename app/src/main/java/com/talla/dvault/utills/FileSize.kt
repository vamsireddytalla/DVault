package com.talla.dvault.utills

import android.os.Build
import java.text.DecimalFormat
import android.os.Environment
import android.os.StatFs
import java.io.File


object FileSize {
    fun floatForm(d: Double): String {
        return DecimalFormat("#.##").format(d)
    }

    var OnLongItemClick:Boolean=false
    var SelectAll:Boolean=false
    var selectedItemIds: MutableSet<Int> = mutableSetOf<Int>()
    const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
    const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"
    const val FILE_NOTIFY_ID = 100
    const val UNLOCK_FILE_NOTIFY_ID = 99
    var FILE_COPYING:Boolean = false

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




}