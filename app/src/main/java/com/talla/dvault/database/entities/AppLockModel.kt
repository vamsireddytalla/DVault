package com.talla.dvault.database.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AppLockModel(
    var hintQuestion:String="What is your Pet Name ?",
    var hintAnswer:String="puppy",
    var userPin:String="12345",
    var isLocked:Boolean=false,
    var timeStamp:String="",
    @PrimaryKey(autoGenerate = false)
    @NonNull
    var appName:String
)