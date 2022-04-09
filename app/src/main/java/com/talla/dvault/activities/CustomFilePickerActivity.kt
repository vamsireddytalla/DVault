package com.talla.dvault.activities

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns._ID
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.talla.dvault.R
import com.talla.dvault.adapters.CustomItemAdapter
import com.talla.dvault.databinding.ActivityCustomFilePickerBinding
import com.talla.dvault.interfaces.CustomClick
import com.talla.dvault.models.CustomItemModel
import com.talla.dvault.utills.FileSize
import com.talla.dvault.viewmodels.CustomItemsVM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CustomFilePickerActivit"

@AndroidEntryPoint
class CustomFilePickerActivity : AppCompatActivity(), CustomClick {
    private lateinit var binding: ActivityCustomFilePickerBinding

    @Inject
    lateinit var glide: RequestManager
    private val viewModel: CustomItemsVM by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomFilePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bundle = intent.extras
        if (bundle != null) {
            val str = bundle.getString(resources.getString(R.string.custom_key))
            binding.titleScrnTitle.text = "Select $str"
            FileSize.customLongItemClick=false
            FileSize.selectedCustomItems.clear()
        }


        lifecycleScope.launch {
            val res: List<CustomItemModel> = viewModel.loadItems()
            Log.d(TAG, "onCreate: ${res.toString()}")
            binding.itemsRCV.layoutManager = LinearLayoutManager(this@CustomFilePickerActivity)
            binding.itemsRCV.adapter = CustomItemAdapter(
                this@CustomFilePickerActivity,
                res,
                glide,
                this@CustomFilePickerActivity
            )
        }

        binding.backbtn.setOnClickListener {
            onBackPressed()
        }

        binding.done.setOnClickListener {
            Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    override fun onBackPressed() {
        FileSize.customLongItemClick=false
        FileSize.selectedCustomItems.clear()
        super.onBackPressed()
    }


    fun getDocuments() {
        val pdf = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf")
        val doc = MimeTypeMap.getSingleton().getMimeTypeFromExtension("doc")
        val docx = MimeTypeMap.getSingleton().getMimeTypeFromExtension("docx")
        val xls = MimeTypeMap.getSingleton().getMimeTypeFromExtension("xls")
        val xlsx = MimeTypeMap.getSingleton().getMimeTypeFromExtension("xlsx")
        val ppt = MimeTypeMap.getSingleton().getMimeTypeFromExtension("ppt")
        val pptx = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pptx")
        val txt = MimeTypeMap.getSingleton().getMimeTypeFromExtension("txt")
        val rtx = MimeTypeMap.getSingleton().getMimeTypeFromExtension("rtx")
        val rtf = MimeTypeMap.getSingleton().getMimeTypeFromExtension("rtf")
        val html = MimeTypeMap.getSingleton().getMimeTypeFromExtension("html")

        //Table

        //Table
        val table = MediaStore.Files.getContentUri("external")
        val table2 = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        //Column
        //Column
        val column = arrayOf(MediaStore.Files.FileColumns.DATA)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        //Where
        val where = (MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?"
                + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?")
        //args
        //args
        val args = arrayOf(pdf, doc, docx, xls, xlsx, ppt, pptx, txt, rtx, rtf, html)

        val fileCursor: Cursor? = contentResolver.query(table, projection, where, args, null)
        var photos = mutableListOf<CustomItemModel>()
        fileCursor?.use { cursor ->
            val columnId = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val displayName =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val width = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val height = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val size = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val date = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                //your code
                val id = cursor.getLong(columnId)
                val displayName = cursor.getString(displayName)
                val width = cursor.getInt(width)
                val height = cursor.getInt(height)
                val size = cursor.getInt(size)
                val contentUris: Uri = ContentUris.withAppendedId(
                    table,
                    id,
                )
                val dateCreated = cursor.getLong(date)
                photos.add(
                    CustomItemModel(
                        id,
                        displayName,
                        "",
                        contentUris.toString(),
                        size,
                        dateCreated.toString(),
                        null
                    )
                )
                Log.d(TAG, "getDocuments: ${photos.toString()}")
            }
        }
        Log.d(TAG, "getDocuments: ${photos.toString()}")
        Toast.makeText(this, "${photos.toString()}", Toast.LENGTH_SHORT).show()
    }

    override fun itemClick(itemsList: MutableSet<CustomItemModel>) {
        if (!itemsList.isEmpty()) {
            binding.titleScrnTitle.text = "${itemsList.size} Selected"
            binding.done.visibility=View.VISIBLE
        }else{
            binding.titleScrnTitle.text="Select Audios"
            binding.done.visibility=View.GONE
        }
    }


}