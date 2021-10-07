package com.talla.dvault.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.talla.dvault.database.entities.User

@Dao
interface DVaultDao
{
    @Insert
    suspend fun insertUserDetails(userDetails:User):Long

    @Query("SELECT * FROM User ORDER BY userloginTime DESC LIMIT 1")
    fun getUserDetails():User

    @Query("SELECT * FROM User WHERE userloginTime = (SELECT MAX(userloginTime) FROM User) ")
    fun getUserLastLogin():User

    @Delete
    suspend fun deleteUserData(user: User)

}