package com.talla.dvault.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(indices = [Index(value = ["folderName", "folderCatType"], unique = true)])
data class FolderTable(
    @PrimaryKey(autoGenerate = true)
    var folderId:Int=0,
    var folderName:String="",
    var folderCreatedAt:String="",
    var folderCatType:String="",
    var folderServerId:String="",
    var folderTrash:Boolean=false
)
