package com.talla.dvault.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.talla.dvault.R
import com.talla.dvault.activities.ItemsActivity
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.databinding.FolderCardBinding
import com.talla.dvault.interfaces.FolderItemClick
import com.talla.dvault.utills.DateUtills

class FoldersAdapter(val folderCat:String,val mContext:Context,val onOptionClick:FolderItemClick) : RecyclerView.Adapter<FoldersAdapter.MyViewHolder>()
{

    inner class MyViewHolder(binding: FolderCardBinding) : RecyclerView.ViewHolder(binding.root) {
        var mbinding: FolderCardBinding? = null

        init {
            this.mbinding = binding
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<FolderTable>() {
        override fun areItemsTheSame(oldItem: FolderTable, newItem: FolderTable): Boolean {
            return oldItem.folderId == newItem.folderId
        }

        override fun areContentsTheSame(oldItem: FolderTable, newItem: FolderTable): Boolean {
            return oldItem == newItem
        }
    }

    var differ = AsyncListDiffer(this, diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = FolderCardBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val folderObj = differ.currentList[position]
        holder.mbinding?.apply {
            folderName.text = folderObj.folderName
            folderCreatedAt.text = DateUtills.convertMilToDate(mContext,folderObj.folderCreatedAt.toLong())
            setCustomFolderIcon(this)

            threeDots?.setOnClickListener{
                val popupMenu = PopupMenu(mContext, threeDots)
                popupMenu.inflate(R.menu.folder_menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.rename -> onOptionClick.onMenuItemClick(folderObj.folderName,mContext.getString(R.string.rename),folderObj.folderId)
                        R.id.delete -> onOptionClick.onMenuItemClick(folderObj.folderName,mContext.getString(R.string.delete),folderObj.folderId)
                    }
                    false
                }
                popupMenu.show()
            }

            folderCard.setOnClickListener {
                val io=Intent(mContext,ItemsActivity::class.java)
                Log.d("FolderName", "onBindViewHolder: ${folderObj}")
                io.putExtra(mContext.getString(R.string.key),folderObj)
                mContext.startActivity(io)
            }


        }
    }

    fun setCustomFolderIcon(folderCardBinding: FolderCardBinding) {
        var selectedColor: Int? =null
        when(folderCat){
            "Img" -> {
                selectedColor= R.color.light_pink
                folderCardBinding.folderLogo.setImageResource(R.drawable.ic_image_icon)
            }
            "Aud" -> {
                selectedColor= R.color.light_yellow
                folderCardBinding.folderLogo.setImageResource(R.drawable.ic_audio_icon)
            }
            "Doc" -> {
                selectedColor= R.color.light_blue
                folderCardBinding.folderLogo.setImageResource(R.drawable.ic_doc_icon)
            }
            "Vdo" -> {
                selectedColor= R.color.light_violet
                folderCardBinding.folderLogo.setImageResource(R.drawable.ic_videos_icon)
            }
        }
        folderCardBinding.folderLogo.setColorFilter(ContextCompat.getColor(mContext,selectedColor!!))
        DrawableCompat.setTint(DrawableCompat.wrap(folderCardBinding.folderCard.background), ContextCompat.getColor(mContext,selectedColor!!))



    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }


}