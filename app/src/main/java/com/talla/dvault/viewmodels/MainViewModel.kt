package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.*
import com.talla.dvault.repositories.Resource
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: VaultRepository,
    private val dao: DVaultDao
) : ViewModel() {
    private var dashMutableData: LiveData<List<CategoriesModel>> =
        MutableLiveData<List<CategoriesModel>>()
    private var dashBoardCountMutableData: LiveData<List<DashBoardCount>> = MutableLiveData()

    init {
        Log.d(TAG, " Init Executed ")
        dashMutableData = repository.getDashBoardData()
    }

    fun getLiveData() = repository.getDashBoardData()

    fun getDashBoardCount(): LiveData<List<DashBoardCount>> {
         dashBoardCountMutableData = repository.getDashBoardCount()
        return dashBoardCountMutableData
    }


    suspend fun getUserObj(): User {
        val userData = repository.getUserData()
        Log.d(TAG, "getUserObj: ${userData.toString()}")
        return userData
    }

    suspend fun insertData(user: User): Long {
        val res: Deferred<Long> = viewModelScope.async {
            repository.insertUser(user)
        }
        return res.await()
    }

    suspend fun insertCatItem(catList: ArrayList<CategoriesModel>) {
        val res: Deferred<Unit> = viewModelScope.async(Dispatchers.Default) {
            try {
                repository.insertCatList(catList)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, "insertUpdateCatList: ${e.message}")
            }
        }
        res.await()
    }


    suspend fun insertFoldertList(folderList: ArrayList<FolderTable>) {
        val res: Deferred<Unit> = viewModelScope.async(Dispatchers.Default) {
            try {
                repository.insertFoldertList(folderList)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, "insertFoldertList: ${e.message}")
            }
        }
        res.await()
    }

    suspend fun insertItemsList(itemsList: ArrayList<ItemModel>) {
        val res: Deferred<Unit> = viewModelScope.async(Dispatchers.Default) {
            try {
                repository.insertItemsList(itemsList)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, "insertItemsList: ${e.message}")
            }
        }
        res.await()
    }

    suspend fun updateCatItem(catModel: CategoriesModel): Int {
        var res: Deferred<Int> = viewModelScope.async {
            try {
                repository.updateCategory(catModel)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("MainActiivty", "insertUpdateCatList: ${e.message}")
            }
        }
        return res.await()
    }


    suspend fun updateUser(user: User): Long {
        var res: Deferred<Long> = viewModelScope.async(Dispatchers.IO) {
            repository.updateUser(user)
        }
        return res.await()
    }

    suspend fun checkIsUserExist(userEmail: String): Int {
        var res: Deferred<Int> = viewModelScope.async {
            repository.checkIsUserExist(userEmail)
        }
        return res.await()
    }


//    suspend fun getAppLockStatus() = viewModelScope.launch(Dispatchers.Default) {
//        mutableLiveData.postValue(repository.getApplockState().value)
//    }


    suspend fun isLockedOrNot(): Boolean {
        var res: Deferred<Boolean> = viewModelScope.async(Dispatchers.IO) {
            repository.isLockOrNot()
        }
        return res.await()
    }

    suspend fun isLoggedInPerfectly(): Int {
        var res: Deferred<Int> = viewModelScope.async(Dispatchers.IO) {
            repository.isLoggedInPerfectly()
        }
        return res.await()
    }

    suspend fun checkEnteredPassword(password: String): Int {
        val res: Deferred<Int> = viewModelScope.async(Dispatchers.IO) {
            repository.checkPassword(password)
        }
        return res.await()
    }

    suspend fun getCategoriesDataIfServIdNull(): List<CategoriesModel> {
        val res: Deferred<List<CategoriesModel>> = viewModelScope.async(Dispatchers.IO) {
            repository.getCategoriesDataIfServIdNull()
        }
        return res.await()
    }

    suspend fun getCategoriesData(): List<CategoriesModel> {
        val res: Deferred<List<CategoriesModel>> = viewModelScope.async(Dispatchers.IO) {
            repository.getCategoriesData()
        }
        return res.await()
    }

    suspend fun getFoldersDataList(): List<FolderTable> {
        val res: Deferred<List<FolderTable>> = viewModelScope.async(Dispatchers.IO) {
            repository.getFoldersDataList()
        }
        return res.await()
    }

    suspend fun getCategoriesIfNotEmpty(): List<CategoriesModel> {
        return repository.getCategoriesIfNotEmpty()
    }

    suspend fun checkPoint(): Int {
        return repository.checkPoint()
    }

    suspend fun updateCatServId(catId: String, servId: String, parentId: String): Int {
        return repository.updateCatServId(catId, servId, parentId)
    }

    suspend fun getDbFilesList(): List<CategoriesModel> {
        return repository.getDbFilesList()
    }

    suspend fun deleteParticularCat(catId: String) {
        repository.deleteParticularCat(catId)
    }


    suspend fun getDbServerFolderId(catId: String): CategoriesModel {
        return repository.getDbServerFolderId(catId)
    }

    suspend fun deletAllAppData() {
        dao.deleteAppLockTable()
        dao.deleteFolderTable()
        dao.deleteItemTable()
        dao.deleteUserTable()
        dao.resetCategoriesTable()
    }

    suspend fun resetCatServerId() {
        viewModelScope.async(Dispatchers.IO) {
            dao.resetCategoriesTable()
        }

    }

}
