package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "FoldersViewModel"
@HiltViewModel
class FoldersViewModel @Inject constructor(private val repository: VaultRepository):ViewModel()
{
    private var foldersMutableData:LiveData<List<FolderTable>> = MutableLiveData<List<FolderTable>>()
    init {
        Log.d(TAG, "FoldersViewModel Init Called")
    }

    suspend fun createNewFolder(folderTable: FolderTable):Long
    {
        return repository.createNewFolder(folderTable)
    }

    suspend fun checkDataANdCreateFolder(folderName: String,folderCreatedAt:String,catType: String):Long
    {
       return repository.checkDataANdCreateFolder(folderName,folderCreatedAt,catType)
    }

    fun getFoldersData(catType:String):LiveData<List<FolderTable>>
    {
        foldersMutableData=repository.getFoldersData(catType)
        return foldersMutableData
    }

    suspend fun getItemsBasedOnFolderId(folderId:String):List<ItemModel>
    {
        val res: Deferred<List<ItemModel>> = viewModelScope.async(Dispatchers.IO) {
            repository.getItemsBasedOnFolderId(folderId)
        }
        return res.await()
    }


    suspend fun renameFolder(folderName:String,folderId:Int):Int
    {
       return repository.renameFolder(folderName,folderId)
    }

    suspend fun updateFolderIfNotExists(folderName:String,folderId:Int):Int
    {
       return repository.updateFolderIfNotExists(folderName,folderId)
    }

    suspend fun updateFolderServIdBasedOnFolderId(folderId:String,folderServID:String):Int
    {
        return repository.updateFolderServIdBasedOnFolderId(folderId,folderServID)
    }

    suspend fun deleteFolder(folderId: Int)
    {
        viewModelScope.launch(Dispatchers.IO){
            try {
                repository.deleteFolder(folderId)
                repository.deleteItemBasedOnFolderId(folderId)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteItem(itemModel:ItemModel,tag:String)
    {
        var respo=viewModelScope.async(Dispatchers.Default) {
            val file= File(itemModel.itemCurrentPath)
            if (file.exists()){
                val isDeleted=file.delete()
                if (isDeleted) {
                    repository.deleteItem(itemModel.itemId)
                }
            }else{
                repository.deleteItem(itemModel.itemId)
            }
        }
    }

    suspend fun getFolderObjWithFolderID(folderId: String):FolderTable
    {
        val res: Deferred<FolderTable> = viewModelScope.async(Dispatchers.IO) {
            repository.getFolderObjWithFolderID(folderId)
        }
        return res.await()
    }

}