package com.talla.dvault.database.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DeleteFilesTable(
    @PrimaryKey
    val itemUri: String,
    val catType:String)