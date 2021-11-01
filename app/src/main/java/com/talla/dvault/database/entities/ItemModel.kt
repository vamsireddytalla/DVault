package com.talla.dvault.database.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class ItemModel(
    @PrimaryKey(autoGenerate = true)
    val itemId:Int=0,
    val itemName:String="",
    val itemSize:String="",
    val itemCreatedAt:String="",
    val itemMimeType:String="",
    val itemOriPath:String="",
    val itemCurrentPath:String="",
    val serverId:String="",
    val folderId:String=""
):Serializable{
    @Ignore
    var isSelected:Boolean=false
}