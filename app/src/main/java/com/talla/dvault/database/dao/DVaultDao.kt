package com.talla.dvault.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.talla.dvault.database.entities.*

@Dao
interface DVaultDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertUserDetails(userDetails: User): Long

    @Insert(onConflict = REPLACE)
    suspend fun updateUser(userDetails: User): Long

    @Query("Update CategoriesModel Set totalItems=:count where categoryName='Image'")
    suspend fun changePhotosCount(count: Int)

    @Query("SELECT * FROM User ORDER BY userloginTime DESC LIMIT 1")
    suspend fun getUserDetails(): User

    @Query("SELECT count(*) FROM User Where userEmail=:userEmail")
    suspend fun checkIsUserExist(userEmail:String): Int

    @Query("SELECT * FROM User WHERE userloginTime = (SELECT MAX(userloginTime) FROM User) ")
    fun getUserLastLogin(): User

    @Delete
    suspend fun deleteUserData(user: User)

    @Insert(onConflict = REPLACE)
    suspend fun insertUserSecurity(appLockModel: AppLockModel): Long

    @Insert
    suspend fun insertDefaultCatData(categoriesModel: List<CategoriesModel>)

    //settings screen quries
    @Query("Select * from AppLockModel order by timeStamp Desc Limit 1")
    fun getApplockState(): LiveData<AppLockModel>

    @Query("Select * from ItemModel Where serverId Is Null or serverId=''")
    fun checkDataAndGetCount(): LiveData<List<ItemModel>>

    @Query("Select * from AppLockModel order by timeStamp Desc Limit 1")
    suspend fun getAppLockModel(): AppLockModel

    @Query("Select isLocked from AppLockModel ORDER BY timeStamp DESC LIMIT 1")
    fun isLockedOrNot(): Boolean

    @Query("Update AppLockModel Set isLocked=:vales")
    suspend fun lockChange(vales: Boolean): Int

    @Insert(onConflict = REPLACE)
    suspend fun saveAppLockData(appLockModel: AppLockModel): Long

    @Query("SELECT count(*) FROM AppLockModel WHERE userPin=:password")
    suspend fun checkPassword(password: String): Int

    @Query("Select count(*) FROM AppLockModel Where hintQuestion=:question AND hintAnswer=:answer")
    suspend fun checkQuesAndAns(question: String, answer: String): Int

    @Query("Delete from AppLockModel")
    suspend fun deleteAppLock(): Int

    //dashboard data
    @Query("Select * from categoriesmodel")
    fun getDashBoardData(): LiveData<List<CategoriesModel>>

    @Insert
    suspend fun createNewFolder(folderTable: FolderTable)

    @Query("INSERT INTO FolderTable (folderName, folderCreatedAt, folderCatType) SELECT * FROM (SELECT :folderName, :folderCreatedAt, :catType) AS tmp WHERE NOT EXISTS (SELECT :folderName FROM FolderTable WHERE folderName = :folderName and folderCatType=:catType) LIMIT 1;")
    suspend fun checkDataANdCreateFolder(
        folderName: String,
        folderCreatedAt: String,
        catType: String
    ): Long

    @Query("Select * from FolderTable where folderCatType=:catType")
    fun getFoldersData(catType: String): LiveData<List<FolderTable>>

    @Query("Update FolderTable Set folderName=:folderName where folderId=:FolderId")
    suspend fun renameFolderName(folderName: String, FolderId: Int): Int


    @Query("Update FolderTable Set folderName = :folderName Where Not Exists (Select folderName from FolderTable  Where folderId=:folderId);")
    suspend fun updateFolderIfNotExists(folderName: String, folderId: Int): Int

    @Query("Delete from FolderTable where folderId=:folderId")
    suspend fun deleteFolder(folderId: Int)

    //Items Data
    @Insert
    suspend fun insertItemsData(itemsList: List<ItemModel>)

    @Insert
    suspend fun insertSingleItem(itemsList: ItemModel)


    @Query("Select * from ItemModel Where itemMimeType=:catType AND folderId=:folderId")
    fun getItemsBasedOnCatType(catType: String, folderId: Int): LiveData<List<ItemModel>>

    @Query("Delete from ItemModel where itemId=:itemId")
    suspend fun deleteItem(itemId: Int)

    @Query("Select * from ItemModel Where (itemMimeType=:categoryType AND (serverId Is Null OR serverId=''))")
    fun getBRItems(categoryType: String): List<ItemModel>

    @Query("Select * from ItemModel Where itemMimeType=:catType AND serverId Is Not Null And serverId!=''")
    fun getRBItems(catType: String): List<ItemModel>

    @Query("Update ItemModel Set serverId=:serverId where itemId=:itemId")
    suspend fun updateItemServerId(serverId: String, itemId: Int): Int

    @Query("Update CategoriesModel Set totalItems=totalItems-1 Where catId=:catId")
    suspend fun removeCatItemCount(catId: String)

    @Query("Update CategoriesModel Set totalItems=totalItems+1 Where catId=:catId")
    suspend fun addCatItemCount(catId: String)

    //delete all tables
    @Query("Delete from ApplockModel")
    suspend fun deleteAppLockTable()

    @Query("Delete from ApplockModel")
    suspend fun deleteCategoriesTable()

    @Query("Delete from FolderTable")
    suspend fun deleteFolderTable()

    @Query("Delete from ItemModel")
    suspend fun deleteItemTable()

    @Query("Delete from User")
    suspend fun deleteUserTable()

    @Query("Update CategoriesModel Set totalItems=0")
    suspend fun resetCategoriesTable()


}