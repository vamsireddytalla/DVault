package com.talla.dvault.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.talla.dvault.R
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.databinding.FileItemBinding
import com.talla.dvault.interfaces.ItemAdapterClick
import android.graphics.drawable.GradientDrawable
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.widget.PopupMenu
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.utills.FileSize


private const val TAG = "ItemsAdapter"

class ItemsAdapter(
    val mContext: Context,
    var itemModelList: List<ItemModel>,
    var glide: RequestManager,
    val onclickListner: ItemAdapterClick
) :
    RecyclerView.Adapter<ItemsAdapter.MyViewHolder>() {


    inner class MyViewHolder(binding: FileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var mbinding: FileItemBinding? = null

        init {
            this.mbinding = binding
            mbinding?.item?.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View?): Boolean {
                    FileSize.OnLongItemClick = true

                    var itemPosition = adapterPosition
                    var obj: ItemModel = itemModelList.get(itemPosition)
                    if (obj.isSelected) {
                        obj.isSelected = false
                        FileSize.selectedUnlockItems.remove(obj)
                    } else {
                        obj.isSelected = true
                        FileSize.selectedUnlockItems.add(obj)
                    }

                    if (FileSize.selectedUnlockItems.isEmpty()) {
                        FileSize.OnLongItemClick = false
                        FileSize.SelectAll = false
                    }

                    notifyItemChanged(itemPosition, obj)
                    onclickListner.onItemClick(FileSize.selectedUnlockItems)

                    return true
                }
            })


            mbinding?.item?.setOnClickListener {
                if (FileSize.OnLongItemClick) {
                    var itemPosition = adapterPosition
                    var obj: ItemModel = itemModelList.get(itemPosition)
                    if (obj.isSelected) {
                        obj.isSelected = false
                        FileSize.selectedUnlockItems.remove(obj)
                    } else {
                        obj.isSelected = true
                        FileSize.selectedUnlockItems.add(obj)
                    }

                    if (FileSize.selectedUnlockItems.isEmpty()) {
                        FileSize.OnLongItemClick = false
                        FileSize.SelectAll = false
                    }

                    notifyItemChanged(itemPosition, obj)
                    onclickListner.onItemClick(FileSize.selectedUnlockItems)

                }
            }


        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        var inflater = LayoutInflater.from(parent.context)
        val binding = FileItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val itemObj = itemModelList.get(position)
        holder.mbinding?.apply {
            itemName.text = itemObj.itemName
            createdAndSize.text = DateUtills.convertMilToDate(mContext,itemObj.itemCreatedAt.toLong()) + " - " + FileSize.bytesToHuman(itemObj.itemSize.toLong())
            //MICRO_KIND, size: 96 x 96 thumbnail
            if (itemObj.itemCatType=="Vdo")
            {
                val bmThumbnail = ThumbnailUtils.createVideoThumbnail(itemObj.itemCurrentPath, MediaStore.Images.Thumbnails.MICRO_KIND);
                thumbNail.setImageBitmap(bmThumbnail)
            }else{
                glide.load(itemObj.itemCurrentPath).into(thumbNail)
            }


            if (!itemObj.isSelected) {
                Log.d(TAG, "Unselected")
                item.setBackgroundColor(Color.TRANSPARENT)
                checkBoxe.visibility = View.GONE
                threeDots.visibility = View.VISIBLE
            } else {
                checkBoxe.visibility = View.VISIBLE
                threeDots.visibility = View.GONE
                Log.d(TAG, "Selected")
                var intColorCode: Int = 0
                when (itemObj.itemCatType) {
                    "Img" -> {
                        intColorCode = mContext.resources.getColor(R.color.light_pink)
                    }
                    "Aud" -> {
                        intColorCode = mContext.resources.getColor(R.color.light_yellow)
                    }
                    "Doc" -> {
                        intColorCode = mContext.resources.getColor(R.color.light_blue)
                    }
                    "Vdo" -> {
                        intColorCode = mContext.resources.getColor(R.color.light_violet)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    checkBoxe.buttonTintList = ColorStateList.valueOf(intColorCode)
                } else {
                    checkBoxe.setBackgroundColor(intColorCode)
                }

                //use a GradientDrawable with only one color set, to make it a solid color
                val border = GradientDrawable()
                border.setColor(-0x1) //white background
                border.cornerRadius = 20F
                border.setColor(mContext.resources.getColor(R.color.card_bg_color))
                border.setStroke(5, intColorCode) //black border with full opacity
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    item.setBackgroundDrawable(border)
                } else {
                    item.setBackground(border)
                }
                checkBoxe.isChecked = true
                item.alpha = 0.8F
            }
            Log.d(TAG, "onBindViewHolder: Called")

            threeDots.setOnClickListener{
                val popupMenu = PopupMenu(mContext, threeDots)
                popupMenu.inflate(R.menu.item_menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.delete -> onclickListner.deleteParticularItem(itemObj)
                    }
                    false
                }
                popupMenu.show()
            }


        }



    }


    override fun getItemCount(): Int {
        return itemModelList.size
    }

    fun setListData(list: List<ItemModel>) {
        itemModelList = list
        notifyDataSetChanged()
    }


}