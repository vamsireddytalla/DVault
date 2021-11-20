package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.database.relations.FolderAndItem
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import javax.inject.Inject

private const val TAG = "ItemViewModel"
@HiltViewModel
class ItemViewModel @Inject constructor(private val repository:VaultRepository) :ViewModel()
{
    private var itemsMutableLiveData: LiveData<List<ItemModel>> = MutableLiveData()
    private var itemsWithFolderLiveData: LiveData<FolderAndItem> = MutableLiveData()
    private var selectedItems: MutableLiveData<String> = MutableLiveData()


    init {
        Log.d(TAG, "ItemViewModel Init Called")
    }

    fun setSelectedItems(s:String){
        selectedItems.value=s
    }

    fun getSelectedMutableData():MutableLiveData<String>{
        return selectedItems
    }


    suspend fun insertItemsData(itemsList:List<ItemModel>)
    {
        var respo=viewModelScope.async(Dispatchers.Default) {
            repository.insertItemsData(itemsList)
        }
    }

    suspend fun insertSingleItem(itemModel: ItemModel){
        var respo=viewModelScope.async(Dispatchers.Default) {
            repository.insertSingleItem(itemModel)
        }
    }


    fun getItemsBasedOnCatType(catType:String,folderId:Int):LiveData<List<ItemModel>>
    {
        itemsMutableLiveData=repository.getItemsBasedOnCatType(catType,folderId)
        return itemsMutableLiveData
    }

    fun getFolderAndItemWithFolderId(folderId:String):LiveData<FolderAndItem>
    {
        repository.getFolderAndItemWithFolderId(folderId).also { itemsWithFolderLiveData = it }
        return itemsWithFolderLiveData
    }


    suspend fun deleteItem(itemModel:ItemModel)
    {
        var respo=viewModelScope.async(Dispatchers.Default) {
            val file=File(itemModel.itemCurrentPath)

            if (file.exists()){
                val isDeleted=file.delete()
                if (isDeleted) repository.deleteItem(itemModel.itemId)
            }else{
              repository.deleteItem(itemModel.itemId)
            }
        }
    }

    suspend fun getFolderObjWithFolderID(folderId:String):FolderTable{
        var res=viewModelScope.async(Dispatchers.Default) {
            repository.getFolderObjWithFolderID(folderId)
        }
       return res.await()
    }


    suspend fun deleteFolder(folderId: Int)
    {
        viewModelScope.launch(Dispatchers.IO){
            try {
                repository.deleteFolder(folderId)
                repository.deleteItemBasedOnFolderId(folderId)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }


}