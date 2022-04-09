package com.talla.dvault.adapters

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.talla.dvault.R
import com.talla.dvault.activities.ExtraActivity
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.databinding.CustomFileItemBinding
import com.talla.dvault.databinding.FileItemBinding
import com.talla.dvault.interfaces.CustomClick
import com.talla.dvault.models.CustomItemModel
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.utills.FileSize
import java.io.File

private const val TAG = "CustomItemAdapter"

class CustomItemAdapter(
    val mContext: Context,
    var customItemList: List<CustomItemModel>,
    var glide: RequestManager,
    var itemAdapterClick: CustomClick
) : RecyclerView.Adapter<CustomItemAdapter.MyViewHolder>() {


    inner class MyViewHolder(var binding: CustomFileItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var mBinding: CustomFileItemBinding? = null

        init {
            mBinding = binding

            mBinding?.let {
                //on long click listner
                it.item.setOnLongClickListener(object : View.OnLongClickListener {
                    override fun onLongClick(v: View?): Boolean {
                        val itemPosition = adapterPosition
                        val obj = customItemList.get(itemPosition)
                        val file = File(obj.contentUri)

                        FileSize.customLongItemClick = true
                        if (obj.isSelected) {
                            obj.isSelected = false
                            FileSize.selectedCustomItems.remove(obj)
                        } else {
                            obj.isSelected = true
                            Log.d(TAG, "onLongClick: ${obj.toString()}")
                            FileSize.selectedCustomItems.add(obj)
                        }
                        if (FileSize.selectedCustomItems.isEmpty()) {
                            FileSize.customLongItemClick = false
                        }
                        notifyItemChanged(itemPosition, obj)
                        itemAdapterClick.itemClick(FileSize.selectedCustomItems)


                        return true
                    }
                })

                //on click listner
                it.item.setOnClickListener {
                    val itemPosition = adapterPosition
                    val obj: CustomItemModel = customItemList.get(itemPosition)
                    val file = File(obj.contentUri)
                    if (FileSize.customLongItemClick) {

                        if (obj.isSelected) {
                            obj.isSelected = false
                            FileSize.selectedCustomItems.remove(obj)
                        } else {
                            obj.isSelected = true
                            FileSize.selectedCustomItems.add(obj)
                        }

                        if (FileSize.selectedCustomItems.isEmpty()) {
                            FileSize.customLongItemClick = false
                        }
                        notifyItemChanged(itemPosition, obj)
                        itemAdapterClick.itemClick(FileSize.selectedCustomItems)


                    }
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CustomFileItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemModel = customItemList.get(position)
        val intColorCode = mContext.resources.getColor(R.color.light_yellow)
        holder.binding.apply {
            val drawableItem = AppCompatResources.getDrawable(mContext, R.drawable.ic_audio_icon)
            thumbNail.setImageDrawable(drawableItem)
            thumbNail.setPadding(30, 30, 30, 30)
            val wrappedDrawable = DrawableCompat.wrap(drawableItem!!)
            DrawableCompat.setTint(wrappedDrawable, intColorCode)

            if (itemModel.isSelected) {
                checkBoxe.visibility = View.VISIBLE

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
                    item.background = border
                }
                checkBoxe.isChecked = true
                item.alpha = 0.8F
            } else {
                item.setBackgroundColor(Color.TRANSPARENT)
                checkBoxe.visibility = View.GONE
            }



            itemName.text = itemModel.name
            createdAndSize.text = DateUtills.convertMilToDate(
                mContext,
                itemModel.dateCreated.toLong()
            ) + " - " + FileSize.bytesToHuman(itemModel.size.toLong())
        }

    }

    override fun getItemCount(): Int {
        return customItemList.size
    }

}