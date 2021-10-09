package com.talla.dvault.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class AppLockModel(
    @PrimaryKey(autoGenerate = false)
    @NonNull
    var hintQuestion:String,
    var hintAnswer:String,
    var userPin:String,
    var userEmail:String
)