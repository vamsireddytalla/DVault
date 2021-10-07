package com.talla.dvault.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import javax.annotation.Nullable

@Entity
data class User(
    @Nullable
    var userName:String,
    var userEmail:String,
    @Nullable
    var userImage:String,
    var userloginTime:String
){
    @PrimaryKey(autoGenerate = true)
    var userId:Int?=null
}
