package com.talla.dvault.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import javax.annotation.Nullable

@Entity(indices = [Index(value = ["userEmail","rowLock"], unique = true)])
data class User(
    @Nullable
    var userName:String,
    var userEmail:String,
    @Nullable
    var userImage:String,
    var userloginTime:String,
    var rowLock:String="DVault"
){
    @PrimaryKey(autoGenerate = true)
    var userId:Int?=null
}
