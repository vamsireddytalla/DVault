package com.talla.dvault.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.*
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

    suspend fun insertCatList(catList:ArrayList<CategoriesModel>)=appDao.insertCartList(catList)

    suspend fun updateCategory(catModel:CategoriesModel):Int{
      return appDao.updateCategory(catModel.serverId,catModel.categoryName,catModel.catId)
    }

    suspend fun updateUser(user:User):Long
    {
        var result=appDao.updateUser(user)
        return result
    }

    fun isLockOrNot(): Boolean = appDao.isLockedOrNot()

    fun isLoggedInPerfectly(): Int = appDao.isLoggedInPerfectly()

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

    suspend fun getCategoriesData()=appDao.getCategoriesData()

    suspend fun getDbFilesList()=appDao.getDbFilesList()

    suspend fun getCategoriesIfNotEmpty()=appDao.getCategoriesIfNotEmpty()

    suspend fun getDbServerFolderId(catId:String)=appDao.getDbServerFolderId(catId)


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

    suspend fun updateCatServId(catId:String,servId:String)=appDao.updateCatServId(catId, servId)

    fun getItemsBasedOnCatType(catType:String,folderId:Int)=appDao.getItemsBasedOnCatType(catType,folderId)

    suspend fun deleteItem(folderId: Int)=appDao.deleteItem(folderId)

    suspend fun deleteParticularCat(catId:String)=appDao.deleteParticularCat(catId)

    suspend fun deleteCategories()=appDao.deleteCategories()

    suspend fun checkPoint()=appDao.checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"));

    suspend fun updateItemServerId(serverId:String,itemId:Int):Int
    {
       return appDao.updateItemServerId(serverId,itemId)
    }

}