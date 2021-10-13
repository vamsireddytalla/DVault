package com.talla.dvault.repositories

import androidx.lifecycle.LiveData
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.AppLockModel
import javax.inject.Inject

class AppLockRepository @Inject constructor(private val appDao: DVaultDao)
{
    fun getApplockState() = appDao.getApplockState()

    suspend fun getAppLockModel()=appDao.getAppLockModel()

    suspend fun saveAppLockData(appLockModel: AppLockModel) = appDao.saveAppLockData(appLockModel)

    suspend fun checkQuesAndAns(question:String,answer:String) =appDao.checkQuesAndAns(question,answer)

    suspend fun disableAppLock():Int = appDao.deleteAppLock()

    suspend fun lockChange(vales:Boolean):Int = appDao.lockChange(vales)

}