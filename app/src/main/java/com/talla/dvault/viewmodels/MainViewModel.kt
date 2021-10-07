package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talla.dvault.database.entities.User
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"
@HiltViewModel
class MainViewModel @Inject constructor(private val repository: VaultRepository) : ViewModel()
{

    init {
        Log.d(TAG, " Init Executed ")
    }

    suspend fun getUserObj() : User
    {
       var userData= repository.getUserData()
        Log.d(TAG, "getUserObj: ${userData.toString()}")
       return userData
    }

    fun insertData(user:User) = viewModelScope.async {
        repository.insertUser(user)
    }


}