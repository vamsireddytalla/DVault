package com.talla.dvault.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.database.entities.User

@Dao
interface DVaultDao
{
    @Insert
    suspend fun insertUserDetails(userDetails:User):Long

    @Query("SELECT * FROM User ORDER BY userloginTime DESC LIMIT 1")
    suspend fun getUserDetails():User

    @Query("SELECT * FROM User WHERE userloginTime = (SELECT MAX(userloginTime) FROM User) ")
    fun getUserLastLogin():User

    @Delete
    suspend fun deleteUserData(user: User)

    @Insert(onConflict = REPLACE)
    suspend fun insertUserSecurity(appLockModel: AppLockModel):Long


}