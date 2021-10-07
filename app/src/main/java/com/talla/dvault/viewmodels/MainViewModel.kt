package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.talla.dvault.database.entities.User
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val TAG = "MainViewModel"
@HiltViewModel
class MainViewModel @Inject constructor(private val repository: VaultRepository) : ViewModel()
{

    init {
        Log.d(TAG, " Init Executed ")
    }

    fun getUserObj() : User
    {
       var userData= repository.getUserData()
        Log.d(TAG, "getUserObj: ${userData.toString()}")
       return userData
    }


}