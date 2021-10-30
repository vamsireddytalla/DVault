package com.talla.dvault.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "FileUnlockService"
private const val CHANNEl_ID = "102"
private const val CHANNEL_NAME = "FILE_PROCESSING_NOTIFICATION"

@AndroidEntryPoint
class FileUnlockService : Service()
{
    @Inject
    lateinit var repository: VaultRepository

    override fun onBind(intent: Intent?): IBinder?
    {
        return null
    }

}