package com.talla.dvault.repositories

import androidx.lifecycle.LiveData
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.User
import javax.inject.Inject

class VaultRepository @Inject constructor(private val appDao:DVaultDao)
{

   fun getUserData():User
   {
       return appDao.getUserDetails()
   }

   suspend fun insertUser(user:User):Long
   {
       var result=appDao.insertUserDetails(user)
       return result
   }

}