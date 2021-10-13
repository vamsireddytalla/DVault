package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.repositories.AppLockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AppLockViewModel"
@HiltViewModel
class AppLockViewModel @Inject constructor(private val repository:AppLockRepository) :ViewModel()
{

    init {
        Log.d(TAG, "AppLockViewModel Init Called")
    }

    suspend fun saveAppLockData(applockModel: AppLockModel): Long {
        return repository.saveAppLockData(applockModel)
    }

    suspend fun checkQuestionAndAns(question:String,answer:String):Int
    {
        var res: Deferred<Int> = viewModelScope.async(Dispatchers.IO) {
            repository.checkQuesAndAns(question, answer)
        }
        return res.await()
    }

}