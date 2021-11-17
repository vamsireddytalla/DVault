package com.talla.dvault.database.entities

import androidx.annotation.Nullable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(indices = [Index(value = ["folderName", "folderCatType"], unique = true)])
data class FolderTable(
    @PrimaryKey(autoGenerate = true)
    val folderId:Int=0,
    val folderName:String="",
    val folderCreatedAt:String="",
    val folderCatType:String="",
    @Nullable
    val folderServerId:String="",
    var folderTrash:Boolean=false)
