package com.talla.dvault.activities

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Observer
import com.bumptech.glide.RequestManager
import com.talla.dvault.R
import com.talla.dvault.databinding.ActivityItemsBinding
import com.talla.dvault.utills.RealPathUtill
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.adapters.FoldersAdapter
import com.talla.dvault.adapters.ItemsAdapter
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.database.entities.SourcesModel
import com.talla.dvault.services.FileCopyService
import java.text.DecimalFormat
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.viewmodels.ItemViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import java.io.*
import javax.xml.transform.Source


private const val TAG = "ItemsActivity"

@AndroidEntryPoint
class ItemsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityItemsBinding
    private var catType: String = ""
    private var folderName: String = ""
    private var folderId: Int? = null
    private lateinit var itemsAdapter: ItemsAdapter
    private lateinit var mService: FileCopyService
    private var mBound: Boolean = false

    @Inject
    lateinit var glide: RequestManager
    private val viewModel: ItemViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var bundle: Bundle? = intent.extras
        if (bundle != null) {
            catType = bundle.getString(resources.getString(R.string.catType))!!
            folderName = bundle.getString(resources.getString(R.string.folderName))!!
            folderId = bundle.getInt(resources.getString(R.string.folderId))
            binding.screenTitle.setText(folderName)
            changeFolderColor(catType!!)
        }

        var res: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    var receivedData: Intent? = result.data
                    Log.d(TAG, "onCreate: ${receivedData?.data?.path.toString()}")
//                    moveFile(RealPathUtill.getRealPath(this,receivedData?.data!!)+"")
                    if (null != receivedData) {
                        if (null != receivedData.clipData) {
                            var itemsList = ArrayList<ItemModel>()
                            var sourceModelList = ArrayList<SourcesModel>()
                            for (i in 0 until receivedData.clipData!!.itemCount) {
                                val uri = receivedData.clipData!!.getItemAt(i).uri
                                var sourceModel: SourcesModel =
                                    SourcesModel(uri.toString(), folderId!!, catType)
                                sourceModelList.add(sourceModel)

//                                var itemModel=fromUriGetRealPath(uri!!)
//                                itemsList.add(itemModel)
                            }
                            startFileCopyService(sourceModelList)
//                            runBlocking {
//                                Log.d(TAG, "Inserting Data Befor Log ${itemsList.toString()}")
//                                viewModel.insertItemsData(itemsList)
//                            }
                        } else {
                            val uri: Uri? = receivedData.data
                            var sourceList = ArrayList<SourcesModel>()
                            var sourceModel: SourcesModel =
                                SourcesModel(uri.toString(), folderId!!, catType)
                            sourceList.add(sourceModel)
                            startFileCopyService(sourceList)

//                            var itemModel=fromUriGetRealPath(uri!!)
//                            var itemsList=ArrayList<ItemModel>()
//                            itemsList.add(itemModel)
//                            runBlocking {
//                                Log.d(TAG, "Inserting Data Befor Log ${itemsList.toString()}")
//                                viewModel.insertItemsData(itemsList)
//                            }
                        }
                    } else {
                        showSnackBar("No File Selected !")
                    }
                } else {
                    Log.d(TAG, "onCreate: No Items Selected")
                }
            }

        binding.plus.setOnClickListener {
            if (!isMyServiceRunning(FileCopyService::class.java)) {
                val openFileIntent = Intent()
                when (catType) {
                    "Img" -> {
                        openFileIntent.setType("image/*")
                        Log.d(TAG, "Img")
                    }
                    "Aud" -> {
                        openFileIntent.setType("audio/*")
                        Log.d(TAG, "Aud")
                    }
                    "Doc" -> {
                        val mimeTypes = arrayOf(
                            "text/csv",
                            "text/comma-separated-values",
                            "application/*",
                            "text/plain"
                        )
                        openFileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        openFileIntent.setType("*/*")
                        Log.d(TAG, "Doc")
                    }
                    "Vdo" -> {
                        openFileIntent.setType("video/*")
                        Log.d(TAG, "Vdo")
                    }
                }
                openFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
                openFileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                openFileIntent.setAction(Intent.ACTION_GET_CONTENT)
                res.launch(Intent.createChooser(openFileIntent, "Select Multiple Items"))
            }else{
                showSnackBar("Already files in Processing...")
            }

        }

        binding.apply {
            itemsAdapter = ItemsAdapter(this@ItemsActivity, glide)
            itemRCV.adapter = itemsAdapter

            backbtn.setOnClickListener {
                finish()
            }

        }

        viewModel.getItemsBasedOnCatType(catType).observe(this, Observer {
            if (it.isEmpty()) {
                binding.nofolderFound.visibility = View.VISIBLE
                binding.itemRCV.visibility = View.GONE
            } else {
                binding.nofolderFound.visibility = View.GONE
                binding.itemRCV.visibility = View.VISIBLE
                binding.itemRCV.isNestedScrollingEnabled=false
                itemsAdapter.setData(it)
                itemsAdapter.differ.submitList(it)
            }
        })

    }

    fun startFileCopyService(sourceList: List<SourcesModel>) {
        var fileService = Intent(this@ItemsActivity, FileCopyService::class.java)
        fileService.putExtra(getString(R.string.fileCopy), sourceList as Serializable)
        fileService.action = "ACTION_START_FOREGROUND_SERVICE"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(fileService)
        } else {
            startService(fileService)
        }
    }



    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    fun getFolderNameBasedOnCat(): String {
        var folderName: String? = null
        when (catType) {
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
        return folderName.toString()
    }

    fun getAllFiles() {
        var catFolderName = getFolderNameBasedOnCat()
        val newdir: File = this.getDir(catFolderName, Context.MODE_PRIVATE)
        val files: Array<File> = newdir.listFiles()
        Log.d("Files", "Size: " + files.size)
        for (i in files.indices) {
            Log.d("Files", "FileName:" + files[i].name)
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

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean
    {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as FileCopyService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

//    override fun onStart() {
//        super.onStart()
//        // Bind to LocalService
//        Intent(this, FileCopyService::class.java).also { intent ->
//            bindService(intent, connection, Context.BIND_AUTO_CREATE)
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        unbindService(connection)
//        mBound = false
//    }



}