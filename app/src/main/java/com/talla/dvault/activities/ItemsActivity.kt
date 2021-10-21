package com.talla.dvault.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import com.talla.dvault.R
import com.talla.dvault.databinding.ActivityItemsBinding
import com.talla.dvault.utills.RealPathUtill
import java.io.File
import android.os.Environment
import com.google.android.material.snackbar.Snackbar
import android.provider.OpenableColumns
import androidx.core.net.toFile
import java.text.DecimalFormat


private const val TAG = "ItemsActivity"
class ItemsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityItemsBinding
    private var catType: String = ""
    private var folderName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var bundle: Bundle? = intent.extras
        if (bundle != null) {
            catType = bundle.getString(resources.getString(R.string.catType))!!
            folderName = bundle.getString(resources.getString(R.string.folderName))!!
            binding.screenTitle.setText(folderName)
            changeFolderColor(catType!!)
            createFolder()
        }

        var res: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    var receivedData: Intent? =result.data
                    Log.d(TAG, "onCreate: ${receivedData?.data?.path.toString()}")
//                    moveFile(RealPathUtill.getRealPath(this,receivedData?.data!!)+"")
                    if (null != receivedData) {
                        if (null !=receivedData.clipData) {
                            for (i in 0 until receivedData.clipData!!.itemCount) {
                                val uri = receivedData.clipData!!.getItemAt(i).uri
                                val fileRealPath=RealPathUtill.getRealPath(this,uri!!)
                                Log.d(TAG, "File size : ${getFileSize(File(fileRealPath))}")
                            }
                        } else {
                            val uri = receivedData.data
                            val fileRealPath=RealPathUtill.getRealPath(this,uri!!)
                            Log.d(TAG, "File size : ${getFileSize(File(fileRealPath))}")
                        }
                    }else{
                        showSnackBar("No File Selected !")
                    }
                } else {
                    Log.d(TAG, "onCreate: No Items Selected")
                }
            }

        binding.plus.setOnClickListener {
            val openFileIntent = Intent()
            when(catType)
            {
                "Img" -> {
                  openFileIntent.setType("image/*")
                    Log.d(TAG, "Img")
                }
                "Aud" -> {
                    openFileIntent.setType("audio/*")
                    Log.d(TAG, "Aud")
                }
                "Doc" -> {
                    val mimeTypes = arrayOf("text/csv", "text/comma-separated-values","application/*","text/plain")
                    openFileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                    openFileIntent.setType("*/*")
                    Log.d(TAG, "Doc")
                }
                "Vdo" -> {
                    openFileIntent.setType("video/*")
                    Log.d(TAG, "Vdo")
                }
            }
            openFileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            openFileIntent.setAction(Intent.ACTION_GET_CONTENT)
            res.launch(Intent.createChooser(openFileIntent,"Select Multiple Items"))
        }

    }


    private val format: DecimalFormat = DecimalFormat("#.##")
    private val MiB = (1024 * 1024).toLong()
    private val GiB = (1024 * 1024 * 1024).toLong()
    private val KiB: Long = 1024

    fun getFileSize(file: File): String? {
        require(file.isFile) { "Expected a file" }
        val length = file.length().toDouble()
        if (length>GiB){
            return format.format(length / GiB).toString() + " Gb"
        }
        else if (length > MiB) {
            return format.format(length / MiB).toString() + " Mb"
        }
        return if (length > KiB) {
            format.format(length / KiB).toString() + " Kb"
        } else format.format(length).toString() + " B"
    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    fun createFolder()
    {
        var folderName: String? = null
        when(catType)
        {
            "Img" -> {
                folderName = "Img"
            }
            "Aud" -> {
                folderName = "Aud"
            }
            "Doc" -> {
                folderName = "Doc"
            }
            "Vdo" -> {
                folderName = "Vdo"
            }
        }
        val newdir: File = this.getDir(folderName, Context.MODE_PRIVATE) //Don't do
        Log.d(TAG, newdir.toString())
        if (!newdir.exists()){
            newdir.mkdirs()
            Log.d(TAG, "createFolder: Creating")
        }else{
            Log.d(TAG, "Existing")
        }
    }

    fun moveFile(fromPath:String)
    {
        val to = File("/data/user/0/com.talla.dvault/app_Sathvik"+ "/svik.jpeg")
        val from=File(fromPath)
        from.renameTo(to)
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