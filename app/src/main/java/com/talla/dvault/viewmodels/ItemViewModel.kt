package com.talla.dvault.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.repositories.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import javax.inject.Inject

private const val TAG = "ItemViewModel"
@HiltViewModel
class ItemViewModel @Inject constructor(private val repository:VaultRepository) :ViewModel()
{
    private var itemsMutableLiveData: LiveData<List<ItemModel>> = MutableLiveData()
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

    fun getItemsBasedOnCatType(catType:String):LiveData<List<ItemModel>>
    {
        itemsMutableLiveData=repository.getItemsBasedOnCatType(catType)
        return itemsMutableLiveData
    }


}