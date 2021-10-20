package com.talla.dvault.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity
data class FolderTable(
    @PrimaryKey(autoGenerate = true)
    var folderId:Int=0,
    var folderName:String="",
    var folderCreatedAt:String="",
    var folderCatType:String=""
)
