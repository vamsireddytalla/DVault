package com.talla.dvault.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity
data class CategoriesModel(
    @PrimaryKey(autoGenerate = false)
    @NotNull
    var catId:String,
    @NotNull
    var categoryName:String,
    var serverId:String,
    var catType:String)