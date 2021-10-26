package com.talla.dvault.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.common.collect.Lists
import com.talla.dvault.R
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.databinding.FileItemBinding
import com.talla.dvault.viewmodels.ItemViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "ItemsAdapter"
class ItemsAdapter(val mContext: Context, var glide: RequestManager) :
    RecyclerView.Adapter<ItemsAdapter.MyViewHolder>() {

    private var isSelectMode: Boolean = false
    private var selectedItems = ArrayList<ItemModel>()
    private var oldItemsList = emptyList<ItemModel>()

    inner class MyViewHolder(binding: FileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var mbinding: FileItemBinding? = null

        init {
            this.mbinding = binding
            mbinding?.item?.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View?): Boolean {
                    isSelectMode = true

                    var itemPosition=adapterPosition
                    var obj: ItemModel = differ.currentList.get(itemPosition)
                    if (selectedItems.get(itemPosition).isSelected) {
                        obj.isSelected = false
                        selectedItems.set(adapterPosition,obj)
                    } else {
                        obj.isSelected = true
                        selectedItems.set(adapterPosition,obj)
                    }

                    differ.submitList(selectedItems)

                    if (selectedItems.size == 0) {
                        isSelectMode = false
                    }

                    return true
                }
            })


            mbinding?.item?.setOnClickListener {
                if (isSelectMode) {
                    var itemPosition=adapterPosition
                    var obj: ItemModel = differ.currentList.get(itemPosition)
                    if (selectedItems.get(itemPosition).isSelected) {
                        obj.isSelected = false
                        selectedItems.set(adapterPosition,obj)
                    } else {
                        obj.isSelected = true
                        selectedItems.set(adapterPosition,obj)
                    }

                    differ.submitList(selectedItems)

                    if (selectedItems.size == 0) {
                        isSelectMode = false
                    }

                }
            }

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

    override fun onBindViewHolder(
        holder: MyViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val itemObj = differ.currentList.get(position)
        holder.mbinding?.apply {
            itemName.text = itemObj.itemName
            createdAndSize.text = itemObj.itemCreatedAt + " - " + itemObj.itemSize
            glide.load(itemObj.itemCurrentPath).into(thumbNail)

            if (!itemObj.isSelected) {
                Log.d(TAG, "Selected")
                item.setBackgroundColor(Color.TRANSPARENT)
            } else {
                Log.d(TAG, "Unselected")
                item.setBackgroundColor(Color.LTGRAY)
            }
            Log.d(TAG, "onBindViewHolder: Called")
        }

    }


    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun setData(newItemModelList:List<ItemModel>)
    {
       selectedItems.addAll(newItemModelList)
    }


}