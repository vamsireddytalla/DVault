package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.User
import com.talla.dvault.repositories.Resource
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: VaultRepository) : ViewModel()
{
    private var dashMutableData:LiveData<List<CategoriesModel>> = MutableLiveData<List<CategoriesModel>>()

    init {
        Log.d(TAG, " Init Executed ")
        dashMutableData=repository.getDashBoardData()
    }

    fun getLiveData() = repository.getDashBoardData()

    suspend fun changePhotosCount(count:Int){
        repository.changePhotosCount(count)
    }


    suspend fun getUserObj(): User {
        var userData = repository.getUserData()
        Log.d(TAG, "getUserObj: ${userData.toString()}")
        return userData
    }

    fun insertData(user: User) = viewModelScope.async {
        repository.insertUser(user)
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

    suspend fun checkEnteredPassword(password: String): Int {
        var res: Deferred<Int> = viewModelScope.async(Dispatchers.IO) {
            repository.checkPassword(password)
        }
        return res.await()
    }


}