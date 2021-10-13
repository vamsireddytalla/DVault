package com.talla.dvault.repositories

import androidx.lifecycle.LiveData
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.database.entities.User
import javax.inject.Inject

class VaultRepository @Inject constructor(private val appDao:DVaultDao)
{

   suspend fun getUserData():User
   {
       return appDao.getUserDetails()
   }

   suspend fun insertUser(user:User):Long
   {
       var result=appDao.insertUserDetails(user)
       return result
   }

    fun isLockOrNot(): Boolean = appDao.isLockedOrNot()

    suspend fun checkPassword(password:String)=appDao.checkPassword(password)

}