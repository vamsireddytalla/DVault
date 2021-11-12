package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.User
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

    init {
        Log.d(TAG, " Init Executed ")
        dashMutableData = repository.getDashBoardData()
    }

    fun getLiveData() = repository.getDashBoardData()


    suspend fun getUserObj(): User {
        var userData = repository.getUserData()
        Log.d(TAG, "getUserObj: ${userData.toString()}")
        return userData
    }

    suspend fun insertData(user: User): Long {
        var res: Deferred<Long> = viewModelScope.async {
            repository.insertUser(user)
        }
        return res.await()
    }

    suspend fun insertCatItem(catList:ArrayList<CategoriesModel>) {
        var res: Deferred<Unit> = viewModelScope.async {
            try {
//                repository.deleteCategories()
                repository.insertCatList(catList)
            }catch (e:Exception){
                e.printStackTrace()
                Log.d(TAG, "insertUpdateCatList: ${e.message}")
            }
        }
        res.await()
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
        var res: Deferred<Int> = viewModelScope.async(Dispatchers.IO) {
            repository.checkPassword(password)
        }
        return res.await()
    }

    suspend fun getCategoriesData(): List<CategoriesModel> {
        return repository.getCategoriesData()
    }


    suspend fun getCategoriesIfNotEmpty(): List<CategoriesModel> {
        return repository.getCategoriesIfNotEmpty()
    }

    suspend fun checkPoint(): Int {
        return repository.checkPoint()
    }

    suspend fun updateCatServId(catId:String,servId:String):Int {
        return repository.updateCatServId(catId,servId)
    }

    suspend fun getDbFilesList():List<CategoriesModel> {
        return repository.getDbFilesList()
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

}
