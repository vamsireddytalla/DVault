package com.talla.dvault.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.ItemModel

data class FolderAndItem(@Embedded val folderTable: FolderTable,
                         @Relation(parentColumn = "folderId",entityColumn = "folderId")
                         val itemModelList: List<ItemModel>)
