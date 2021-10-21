package com.talla.dvault.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.databinding.FileItemBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

class ItemsAdapter(val mContext: Context,var glide: RequestManager) : RecyclerView.Adapter<ItemsAdapter.MyViewHolder>()
{

    inner class MyViewHolder(binding: FileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var mbinding: FileItemBinding? = null

        init {
            this.mbinding = binding
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<ItemModel>() {
        override fun areItemsTheSame(oldItem: ItemModel, newItem: ItemModel): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: ItemModel, newItem: ItemModel): Boolean {
            return oldItem == newItem
        }
    }

    var differ = AsyncListDiffer(this, diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        var inflater = LayoutInflater.from(parent.context)
        val binding = FileItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemObj = differ.currentList[position]
        holder.mbinding?.apply {
            itemName.text = itemObj.itemName
            createdAndSize.text = itemObj.itemCreatedAt+" "+itemObj.itemSize
            glide.load(itemObj.itemCurrentPath).into(thumbNail)
        }
    }

    override fun getItemCount(): Int {
      return  differ.currentList.size
    }


}