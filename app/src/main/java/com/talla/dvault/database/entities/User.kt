package com.talla.dvault.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    var userId:Int,
    var userName:String,
    var userEmail:String,
    var userImage:String,
    var userloginTime:String
)
