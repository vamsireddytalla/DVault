package com.talla.dvault.repositories

import androidx.lifecycle.LiveData
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.database.entities.FolderTable
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


    suspend fun  changePhotosCount(count:Int)
    {
        appDao.changePhotosCount(count)
    }

    fun isLockOrNot(): Boolean = appDao.isLockedOrNot()

    suspend fun checkPassword(password:String)=appDao.checkPassword(password)

    fun getDashBoardData()=appDao.getDashBoardData()


    suspend fun createNewFolder(folderTable: FolderTable)
    {
        appDao.createNewFolder(folderTable)
    }

    fun getFoldersData(catType:String)=appDao.getFoldersData(catType)


    suspend fun renameFolder(folderName:String,folderId:Int)=appDao.renameFolderName(folderName,folderId)

    suspend fun deleteFolder(folderId: Int)=appDao.deleteFolder(folderId)

}