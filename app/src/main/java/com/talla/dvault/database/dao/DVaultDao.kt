package com.talla.dvault.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.User

@Dao
interface DVaultDao
{
    @Insert
    suspend fun insertUserDetails(userDetails:User):Long

    @Query("Update CategoriesModel Set totalItems=:count where categoryName='Image'")
    suspend fun  changePhotosCount(count:Int)

    @Query("SELECT * FROM User ORDER BY userloginTime DESC LIMIT 1")
    suspend fun getUserDetails():User

    @Query("SELECT * FROM User WHERE userloginTime = (SELECT MAX(userloginTime) FROM User) ")
    fun getUserLastLogin():User

    @Delete
    suspend fun deleteUserData(user: User)

    @Insert(onConflict = REPLACE)
    suspend fun insertUserSecurity(appLockModel: AppLockModel):Long

    @Insert
    suspend fun insertDefaultCatData(categoriesModel: List<CategoriesModel>)

    //settings screen quries
    @Query("Select * from AppLockModel order by timeStamp Desc Limit 1")
    fun getApplockState():LiveData<AppLockModel>

    @Query("Select * from AppLockModel order by timeStamp Desc Limit 1")
    suspend fun getAppLockModel():AppLockModel

    @Query("Select isLocked from AppLockModel ORDER BY timeStamp DESC LIMIT 1")
    fun isLockedOrNot():Boolean

    @Query("Update AppLockModel Set isLocked=:vales")
    suspend fun lockChange(vales:Boolean):Int

    @Insert(onConflict = REPLACE)
    suspend fun saveAppLockData(appLockModel: AppLockModel):Long

    @Query("SELECT count(*) FROM AppLockModel WHERE userPin=:password")
    suspend fun checkPassword(password:String):Int

    @Query("Select count(*) FROM AppLockModel Where hintQuestion=:question AND hintAnswer=:answer")
    suspend fun checkQuesAndAns(question:String,answer:String):Int

    @Query("Delete from AppLockModel")
    suspend fun deleteAppLock():Int

    //dashboard data
    @Query("Select * from categoriesmodel")
    fun getDashBoardData():LiveData<List<CategoriesModel>>

    @Insert
    suspend fun createNewFolder(folderTable: FolderTable)

    @Query("Select * from FolderTable where folderCatType=:catType")
    fun getFoldersData(catType:String):LiveData<List<FolderTable>>

    @Query("Update FolderTable Set folderName=:folderName where folderId=:FolderId")
    suspend fun renameFolderName(folderName:String,FolderId:Int)

    @Query("Delete from FolderTable where folderId=:folderId")
    suspend fun deleteFolder(folderId: Int)

}