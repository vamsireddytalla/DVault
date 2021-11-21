package com.talla.dvault.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.talla.dvault.database.entities.*
import androidx.sqlite.db.SupportSQLiteQuery

import androidx.room.RawQuery
import com.talla.dvault.database.relations.FolderAndItem


@Dao
interface DVaultDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertUserDetails(userDetails: User): Long

    @Insert(onConflict = REPLACE)
    suspend fun insertUpdateCatList(catList: ArrayList<CategoriesModel>)

    @Update(onConflict = REPLACE)
    suspend fun insertCatItem(catModel: CategoriesModel): Int

    @Insert(onConflict = REPLACE)
    suspend fun insertCartList(catList: ArrayList<CategoriesModel>)

    @Insert(onConflict = REPLACE)
    suspend fun insertFoldertList(folderList: ArrayList<FolderTable>)

    @Insert(onConflict = REPLACE)
    suspend fun insertItemsList(itemList: ArrayList<ItemModel>)

    @Query("Update CategoriesModel Set serverId=:serverId,categoryName=:catName Where catId=:catId")
    suspend fun updateCategory(serverId: String, catName: String, catId: String): Int

    @Insert(onConflict = REPLACE)
    suspend fun updateUser(userDetails: User): Long

    @Transaction
    @Query("SELECT * FROM User ORDER BY userloginTime DESC LIMIT 1")
    suspend fun getUserDetails(): User

    @Transaction
    @Query("SELECT count(*) FROM User Where userEmail=:userEmail")
    suspend fun checkIsUserExist(userEmail: String): Int

    @Transaction
    @Query("SELECT * FROM User WHERE userloginTime = (SELECT MAX(userloginTime) FROM User) ")
    fun getUserLastLogin(): User

    @Delete
    suspend fun deleteUserData(user: User)

    @Insert(onConflict = REPLACE)
    suspend fun insertUserSecurity(appLockModel: AppLockModel): Long

    @Insert
    suspend fun insertDefaultCatData(categoriesModel: List<CategoriesModel>)

    //settings screen quries
    @Transaction
    @Query("Select * from AppLockModel order by timeStamp Desc Limit 1")
    fun getApplockState(): LiveData<AppLockModel>

    @Transaction
    @Query("Select * from ItemModel Where serverId Is Null or serverId=''")
    fun checkDataAndGetCount(): LiveData<List<ItemModel>>

    @Transaction
    @Query("Select * from AppLockModel order by timeStamp Desc Limit 1")
    suspend fun getAppLockModel(): AppLockModel

    @Transaction
    @Query("Select isLocked from AppLockModel ORDER BY timeStamp DESC LIMIT 1")
    fun isLockedOrNot(): Boolean

    @Transaction
    @Query("Select Count(*) from CategoriesModel Where (serverId IS NOT NULL AND serverId!='')")
    fun isLoggedInPerfectly(): Int

    @Query("Update AppLockModel Set isLocked=:vales")
    suspend fun lockChange(vales: Boolean): Int

    @Insert(onConflict = REPLACE)
    suspend fun saveAppLockData(appLockModel: AppLockModel): Long

    @Transaction
    @Query("SELECT count(*) FROM AppLockModel WHERE userPin=:password")
    suspend fun checkPassword(password: String): Int

    @Transaction
    @Query("Select count(*) FROM AppLockModel Where hintQuestion=:question AND hintAnswer=:answer")
    suspend fun checkQuesAndAns(question: String, answer: String): Int

    @Query("Delete from AppLockModel")
    suspend fun deleteAppLock(): Int

    //dashboard data
    @Transaction
    @Query("Select * from categoriesmodel")
    fun getDashBoardData(): LiveData<List<CategoriesModel>>

    @Insert
    suspend fun createNewFolder(folderTable: FolderTable):Long

    @Query("INSERT INTO FolderTable (folderName, folderCreatedAt, folderCatType) SELECT * FROM (SELECT :folderName, :folderCreatedAt, :catType) AS tmp WHERE NOT EXISTS (SELECT :folderName FROM FolderTable WHERE folderName = :folderName and folderCatType=:catType) LIMIT 1;")
    suspend fun checkDataANdCreateFolder(
        folderName: String,
        folderCreatedAt: String,
        catType: String
    ): Long

    @Transaction
    @Query("Select * from FolderTable where folderCatType=:catType")
    fun getFoldersData(catType: String): LiveData<List<FolderTable>>

    @Query("Update FolderTable Set folderName=:folderName where folderId=:FolderId")
    suspend fun renameFolderName(folderName: String, FolderId: Int): Int


    @Query("Update FolderTable Set folderName = :folderName Where Not Exists (Select folderName from FolderTable  Where folderId=:folderId);")
    suspend fun updateFolderIfNotExists(folderName: String, folderId: Int): Int

    @Query("Delete from FolderTable where folderId=:folderId")
    suspend fun deleteFolder(folderId: Int)


    @Query("Delete from ItemModel where folderId=:folderId")
    suspend fun deleteItemBasedOnFolderId(folderId: Int)

    //Items Data
    @Insert
    suspend fun insertItemsData(itemsList: List<ItemModel>)

    @Insert
    suspend fun insertSingleItem(itemsList: ItemModel)

    @Transaction
    @Query("Select * from ItemModel Where itemCatType=:catType AND folderId=:folderId")
    fun getItemsBasedOnCatType(catType: String, folderId: Int): LiveData<List<ItemModel>>

    @Query("Delete from ItemModel where itemId=:itemId")
    suspend fun deleteItem(itemId: Int)

    @Query("Delete from CategoriesModel where catId=:catId")
    suspend fun deleteParticularCat(catId: String)

    @Transaction
    @Query("Select * from ItemModel Where (itemCatType=:categoryType AND (serverId Is Null OR serverId=''))")
    fun getBRItems(categoryType: String): List<ItemModel>

    @Transaction
    @Query("Select * from ItemModel Where itemCatType=:catType")
    fun getRBItems(catType: String): List<ItemModel>

    @Transaction
    @Query("Select * from CategoriesModel Where (serverId IS NULL OR serverId='')")
    suspend fun getCategoriesDataIfServIdNull(): List<CategoriesModel>

    @Transaction
    @Query("Select * from CategoriesModel")
    suspend fun getCategoriesData(): List<CategoriesModel>

    @Transaction
    @Query("Select * from CategoriesModel Where (serverId IS NOT NULL AND serverId!='' AND catType='file/*')")
    suspend fun getCategoriesIfNotEmpty(): List<CategoriesModel>

    @Transaction
    @Query("Select * from CategoriesModel Where (catId=:catId AND (serverId IS NOT NULL OR serverId!='')) Limit 1")
    suspend fun getDbServerFolderId(catId: String): CategoriesModel

    @Transaction
    @Query("Select * from CategoriesModel")
    suspend fun getDbFilesList(): List<CategoriesModel>

    @Transaction
    @Query("Update ItemModel Set serverId=:serverId where itemId=:itemId")
    suspend fun updateItemServerId(serverId: String, itemId: Int): Int

    //delete all tables
    @Query("Delete from ApplockModel")
    suspend fun deleteAppLockTable()

    @Query("Delete from CategoriesModel")
    suspend fun deleteCategoriesTable()

    @Query("Delete from FolderTable")
    suspend fun deleteFolderTable()

    @Query("Delete from ItemModel")
    suspend fun deleteItemTable()

    @Query("Delete from User")
    suspend fun deleteUserTable()

    @Query("Update CategoriesModel Set serverId=''")
    suspend fun resetCategoriesTable()

    @Query("Delete from CategoriesModel")
    suspend fun deleteCategories()

    @Query("Update CategoriesModel Set serverId=:servId,driveRootId=:parentId Where catId=:catId")
    suspend fun updateCatServId(catId: String, servId: String,parentId:String): Int

    @Query("Update FolderTable Set folderServerId=:servId Where folderCatType=:folderCatType")
    suspend fun updateFolderServId(folderCatType: String, servId: String): Int

    @Query("Update FolderTable Set folderServerId=:servId Where folderId=:folderId")
    suspend fun updateFolderServIdBasedOnFolderId(folderId: String, servId: String): Int

    @RawQuery
    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery?): Int

    @Transaction
    @Query("Select * from FolderTable Where folderCatType=:catType")
    suspend fun getFolderAndItemWithCatType(catType: String):FolderAndItem

    @Transaction
    @Query("Select * from FolderTable Where folderId=:folderId")
    fun getFolderAndItemWithFolderId(folderId: String):LiveData<FolderAndItem>

    @Transaction
    @Query("Select serverId from CategoriesModel Where catId=:catId")
    suspend fun getCatServerId(catId: String):String

    @Transaction
    @Query("Select * from FolderTable Where folderCatType=:catId")
    suspend fun getFolderObject(catId: String):FolderTable


    @Transaction
    @Query("Select * from FolderTable Where (folderCatType=:catId and folderId=:folderId)")
    suspend fun getFolderObjBasedOnCatAndFolderID(catId: String,folderId:String):FolderTable

    @Transaction
    @Query("Select * from ItemModel Where folderId=:folderid")
    fun getItemsBasedOnFolderId(folderid:String): List<ItemModel>


    @Transaction
    @Query("Select * from FolderTable")
    fun getFoldersDataList(): List<FolderTable>

    @Transaction
    @Query("Select * from FolderTable Where folderId=:folderId")
    fun getFolderObjWithFolderID(folderId:String):FolderTable


}