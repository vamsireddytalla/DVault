package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.repositories.AppLockRepository
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.MediatorLiveData
import com.talla.dvault.database.entities.ItemModel
import kotlinx.coroutines.*


private const val TAG = "SettingsViewModel"

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: AppLockRepository) :
    ViewModel() {
    private val appLockMutableData = MutableLiveData<AppLockModel>()
    private var appMData = MutableLiveData<AppLockModel>()

    init {
        Log.d(TAG, "SettingsViewModel Init Called")
    }

    suspend fun getAppLockStatus(): LiveData<AppLockModel> {
        var someVal = repository.getApplockState()
        Log.d(TAG, "getAppLockStatus: ${someVal.value?.userPin}")
        appMData.postValue(someVal.value)
        Log.d(TAG, "getAppLockStatus: ${appMData.value}")
        return someVal

    }

    suspend fun getLockData(): LiveData<AppLockModel> {

        return repository.getApplockState()
    }

    suspend fun checkDataAndGetCount(): LiveData<List<ItemModel>> {

        var res: Deferred<LiveData<List<ItemModel>>> = viewModelScope.async(Dispatchers.IO) {
            repository.checkDataAndGetCount()
        }
        return res.await()
    }

    suspend fun lockChange(vales: Boolean): Int {
        return repository.lockChange(vales)
    }

    suspend fun disableAppLock(): Int {
        var res: Deferred<Int> = viewModelScope.async(Dispatchers.IO) {
            repository.disableAppLock()
        }
        return res.await()
    }

    suspend fun getAppLockModel(): AppLockModel {
        var res = viewModelScope.async(Dispatchers.Default) {
            repository.getAppLockModel()
        }
        return res.await()
    }


}