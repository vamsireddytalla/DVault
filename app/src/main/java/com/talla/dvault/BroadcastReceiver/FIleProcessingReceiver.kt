package com.talla.dvault.BroadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import android.widget.Toast
import androidx.core.app.ServiceCompat.stopForeground

class FIleProcessingReceiver :BroadcastReceiver()
{

    override fun onReceive(context: Context?, intent: Intent?) {
        Toast.makeText(context, "Canceled", Toast.LENGTH_SHORT).show()
        val notificationManager = NotificationManagerCompat.from(context!!)
        notificationManager.cancel(1)
    }

}