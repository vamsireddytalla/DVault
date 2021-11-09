package com.talla.dvault.repositories

import androidx.lifecycle.LiveData
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.database.entities.User
import java.lang.Exception
import javax.inject.Inject

class VaultRepository @Inject constructor(private val appDao:DVaultDao)
{

   suspend fun getUserData():User
   {
       return appDao.getUserDetails()
   }

    suspend fun checkIsUserExist(userEmail:String):Int
    {
        return appDao.checkIsUserExist(userEmail = userEmail)
    }

   suspend fun insertUser(user:User):Long
   {
       var result=appDao.insertUserDetails(user)
       return result
   }

    suspend fun updateUser(user:User):Long
    {
        var result=appDao.updateUser(user)
        return result
    }


    suspend fun changePhotosCount(count:Int)
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

    suspend fun checkDataANdCreateFolder(folderName: String,folderCreatedAt:String,catType: String):Long
    {
       return appDao.checkDataANdCreateFolder(folderName,folderCreatedAt,catType)
    }

    fun getFoldersData(catType:String)=appDao.getFoldersData(catType)

    fun getBRItems(catType:String)=appDao.getBRItems(catType)

    fun getRBItems(catType:String)=appDao.getRBItems(catType)


    suspend fun renameFolder(folderName:String,folderId:Int):Int
    {
        try {
           return appDao.renameFolderName(folderName,folderId)
        }catch (ee:Exception){
            return 2067
        }
    }

    suspend fun updateFolderIfNotExists(folderName:String,folderId:Int)=appDao.updateFolderIfNotExists(folderName,folderId)

    suspend fun deleteFolder(folderId: Int)=appDao.deleteFolder(folderId)

    suspend fun insertItemsData(itemsList:List<ItemModel>)=appDao.insertItemsData(itemsList)

    suspend fun insertSingleItem(itemsList:ItemModel)=appDao.insertSingleItem(itemsList)

    suspend fun removeCatItemCount(catId: String)=appDao.removeCatItemCount(catId)

    suspend fun addCatItemCount(catId: String)=appDao.addCatItemCount(catId)

    fun getItemsBasedOnCatType(catType:String,folderId:Int)=appDao.getItemsBasedOnCatType(catType,folderId)

    suspend fun deleteItem(folderId: Int)=appDao.deleteItem(folderId)

    suspend fun updateItemServerId(serverId:String,itemId:Int):Int
    {
       return appDao.updateItemServerId(serverId,itemId)
    }

}