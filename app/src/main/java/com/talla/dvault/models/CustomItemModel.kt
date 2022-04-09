package com.talla.dvault.models

import androidx.room.Ignore
import com.talla.dvault.database.entities.FolderTable
import java.io.Serializable

data class CustomItemModel
    (
    val id: Long,
    val name: String,
    val mimeType: String,
    val contentUri: String,
    val size: Int,
    val dateCreated: String,
    val folderTable: FolderTable?=null
) : Serializable {
    @Ignore
    var isSelected: Boolean = false
}