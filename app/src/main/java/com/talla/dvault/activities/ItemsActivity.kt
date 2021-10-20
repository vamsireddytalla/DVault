package com.talla.dvault.activities

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.talla.dvault.R
import com.talla.dvault.databinding.ActivityItemsBinding

class ItemsActivity : AppCompatActivity()
{
    private lateinit var binding:ActivityItemsBinding
    private var catType:String=""
    private var folderName:String=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var bundle: Bundle? = intent.extras
        if (bundle != null) {
            catType = bundle.getString(resources.getString(R.string.catType))!!
            folderName = bundle.getString(resources.getString(R.string.folderName))!!
            binding.screenTitle.setText(folderName)
            changeFolderColor(catType!!)
        }
    }

    fun changeFolderColor(type: String) {
        var selectedColor: Int? = null
        var screenTitle: String? = null

        when (type) {
            "Img" -> {
                selectedColor = R.color.light_pink
                screenTitle = "Images"
            }
            "Aud" -> {
                selectedColor = R.color.light_yellow
                screenTitle = "Audio"
            }
            "Doc" -> {
                selectedColor = R.color.light_blue
                screenTitle = "Documents"
            }
            "Vdo" -> {
                selectedColor = R.color.light_violet
                screenTitle = "Videos"
            }
        }
        binding.screenTitle.text = screenTitle
        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.plus.background),
            ContextCompat.getColor(this, selectedColor!!)
        )
    }

}