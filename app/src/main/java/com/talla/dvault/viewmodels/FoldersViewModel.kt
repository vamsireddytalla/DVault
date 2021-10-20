package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FoldersViewModel"
@HiltViewModel
class FoldersViewModel @Inject constructor(private val repository: VaultRepository):ViewModel()
{
    private var foldersMutableData:LiveData<List<FolderTable>> = MutableLiveData<List<FolderTable>>()
    init {
        Log.d(TAG, "FoldersViewModel Init Called")
    }

    suspend fun createNewFolder(folderTable: FolderTable)
    {
        repository.createNewFolder(folderTable)
    }

    fun getFoldersData(catType:String):LiveData<List<FolderTable>>
    {
        foldersMutableData=repository.getFoldersData(catType)
        return foldersMutableData
    }


    suspend fun renameFolder(folderName:String,folderId:Int)
    {
        repository.renameFolder(folderName,folderId)
    }

    suspend fun deleteFolder(folderId: Int)
    {
        repository.deleteFolder(folderId)
    }



}