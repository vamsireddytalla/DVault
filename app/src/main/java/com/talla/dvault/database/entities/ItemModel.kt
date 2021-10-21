package com.talla.dvault.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["serverId"], unique = true)])
data class ItemModel(
    @PrimaryKey(autoGenerate = true)
    val itemId:Int,
    val itemName:String,
    val itemSize:String,
    val itemCreatedAt:String,
    val itemMimeType:String,
    val itemOriPath:String,
    val itemCurrentPath:String,
    val serverId:String,
    val folderId:String
)
