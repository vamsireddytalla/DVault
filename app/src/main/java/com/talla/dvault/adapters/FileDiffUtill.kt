package com.talla.dvault.adapters

import androidx.recyclerview.widget.DiffUtil
import com.talla.dvault.database.entities.ItemModel

class FileDiffUtill(
    private val oldList: List<ItemModel>,
    private val newList: List<ItemModel>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].itemId == newList[newItemPosition].itemId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return when{
            oldList[oldItemPosition].itemId != newList[newItemPosition].itemId ->{
                return false
            }
            oldList[oldItemPosition].itemName != newList[newItemPosition].itemName ->{
                return false
            }
            oldList[oldItemPosition].itemSize != newList[newItemPosition].itemSize ->{
                return false
            }
            oldList[oldItemPosition].itemCreatedAt != newList[newItemPosition].itemCreatedAt ->{
                return false
            }
            oldList[oldItemPosition].itemMimeType != newList[newItemPosition].itemMimeType ->{
                return false
            }
            oldList[oldItemPosition].itemOriPath != newList[newItemPosition].itemOriPath ->{
                return false
            }
            oldList[oldItemPosition].itemCurrentPath != newList[newItemPosition].itemCurrentPath ->{
                return false
            }
            oldList[oldItemPosition].serverId != newList[newItemPosition].serverId ->{
                return false
            }
            oldList[oldItemPosition].folderId != newList[newItemPosition].folderId ->{
                return false
            }
            oldList[oldItemPosition].isSelected != newList[newItemPosition].isSelected ->{
                return false
            }
            else -> true
        }
    }

}