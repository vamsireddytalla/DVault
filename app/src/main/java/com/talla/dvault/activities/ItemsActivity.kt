package com.talla.dvault.activities

import android.app.Activity
import android.app.ActivityManager
import android.app.Dialog
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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.RequestManager
import com.talla.dvault.R
import com.talla.dvault.databinding.ActivityItemsBinding
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.adapters.ItemsAdapter
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.database.entities.SourcesModel
import com.talla.dvault.databinding.CopyingFileDialogBinding
import com.talla.dvault.interfaces.ItemAdapterClick
import com.talla.dvault.services.FileCopyService
import com.talla.dvault.utills.FileSize
import com.talla.dvault.viewmodels.ItemViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import java.io.*
import java.lang.Exception

private const val TAG = "ItemsActivity"

@AndroidEntryPoint
class ItemsActivity : AppCompatActivity(), ItemAdapterClick {
    private lateinit var binding: ActivityItemsBinding
    private var catType: String = ""
    private var folderName: String = ""
    private var folderId: Int? = null
    private lateinit var itemsAdapter: ItemsAdapter
    private var itemsList: List<ItemModel> = ArrayList()
//    private var mBound: Boolean = false
    private lateinit var dialog: Dialog
    private var binder: FileCopyService.LocalBinder?=null
    private var serviceBinder: FileCopyService? = null
    private lateinit var copyDialog: CopyingFileDialogBinding


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
            binding.screenTitle.text = folderName
            changeFolderColor(catType)
        }

        var res: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    var receivedData: Intent? = result.data
                    Log.d(TAG, "onCreate: ${receivedData?.data?.path.toString()}")
                    if (null != receivedData) {
                        var sourceList = ArrayList<SourcesModel>()
                        val appPrivatePath: File = this.getDir(folderName, Context.MODE_PRIVATE)
                        if (null != receivedData.clipData) {
                            for (i in 0 until receivedData.clipData!!.itemCount) {
                                val uri = receivedData.clipData!!.getItemAt(i).uri
                                var sourceModel = SourcesModel(uri.toString(), folderId!!, catType)
                                sourceList.add(sourceModel)

                            }
                        } else {
                            val uri: Uri? = receivedData.data
                            var sourceModel = SourcesModel(uri.toString(), folderId!!, catType)
                            sourceList.add(sourceModel)
                        }

                        binder?.startFileCopyingService(sourceList)
                        showFileCopyDialog("Copy")

                        //                        else{
//                            startFileCopyService(sourceList)
//                        }

                    } else {
                        showSnackBar("No File Selected !")
                    }
                } else {
                    Log.d(TAG, "onCreate: No Items Selected")
                }
            }

        binding.plus.setOnClickListener {
            if (!FileSize.FILE_COPYING && !FileSize.UNLOCK_FILE_COPYING) {

                val openFileIntent = Intent()
                when (catType) {
                    "Img" -> {
                        openFileIntent.type = "image/*"
                        Log.d(TAG, "Img")
                    }
                    "Aud" -> {
                        openFileIntent.type = "audio/*"
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
                        openFileIntent.type = "*/*"
                        Log.d(TAG, "Doc")
                    }
                    "Vdo" -> {
                        openFileIntent.type = "video/*"
                        Log.d(TAG, "Vdo")
                    }
                }
                openFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
                openFileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                openFileIntent.setAction(Intent.ACTION_GET_CONTENT)
                res.launch(Intent.createChooser(openFileIntent, "Select Multiple Items"))


            } else {
                showSnackBar("Already files in Processing...")
                binder?.let { showFileCopyDialog("Copy") }
            }

        }

        binding.apply {
            itemsAdapter = ItemsAdapter(this@ItemsActivity, itemsList, glide, this@ItemsActivity)
            itemRCV.adapter = itemsAdapter

            backbtn.setOnClickListener {
                finish()
            }

        }

        viewModel.getItemsBasedOnCatType(catType, folderId!!).observe(this, Observer {
            Log.d(TAG, "ItemsActivty Observer")
            if (it.isEmpty()) {
                binding.nofolderFound.visibility = View.VISIBLE
                binding.itemRCV.visibility = View.GONE
            } else {
                binding.nofolderFound.visibility = View.GONE
                binding.itemRCV.visibility = View.VISIBLE
                binding.itemRCV.isNestedScrollingEnabled = false
                Log.d(TAG, "onCreate: ${it.toString()}")
                itemsList = it
                itemsAdapter.setListData(itemsList)
            }
        })

        binding.selectAll.setOnClickListener {
            selectALlCall()
        }

        binding.unlock.setOnClickListener {
            if (FileSize.selectedUnlockItems.isNotEmpty() && !FileSize.UNLOCK_FILE_COPYING && !FileSize.FILE_COPYING){
                binder?.unlockFilesService(FileSize.selectedUnlockItems)
                FileSize.SelectAll=true
                selectALlCall()
                binding.unlock.visibility = View.GONE
                binding.selectAll.visibility = View.GONE
                showFileCopyDialog("Unlock")
            }else {
                showSnackBar("Unlocking files already in Processing...")
                binder?.let { showFileCopyDialog("Unlock") }
            }
        }

    }

    fun selectALlCall(){
        lifecycleScope.async {

            FileSize.SelectAll = !FileSize.SelectAll
            FileSize.OnLongItemClick = FileSize.SelectAll

            for (itemModel in itemsList) {
                itemModel.isSelected = FileSize.SelectAll
                if (FileSize.SelectAll) FileSize.selectedUnlockItems.add(itemModel) else {
                    FileSize.selectedUnlockItems.clear()
                    withContext(Dispatchers.Main) {
                        binding.plus.visibility = View.VISIBLE
                        binding.unlock.visibility = View.GONE
                        binding.selectAll.visibility = View.GONE
                    }
                }
            }

            itemsAdapter.setListData(itemsList)
            Log.d(TAG, "Select ALl Clicked Select All Value  ${FileSize.SelectAll}")
            Log.d(TAG, "Select ALl Clicked OnLon Value   ${FileSize.OnLongItemClick}")
            Log.d(
                TAG,
                "Select ALl CLicked MyItems Ids ${FileSize.selectedUnlockItems.toString()}"
            )
        }
    }

    fun startFileCopyService(sourceList: List<SourcesModel>) {
        var fileService = Intent(this@ItemsActivity, FileCopyService::class.java)
        fileService.putExtra(getString(R.string.fileCopy), sourceList as Serializable)
        fileService.action = FileSize.ACTION_START_FOREGROUND_SERVICE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(fileService)
        } else {
            startService(fileService)
        }
    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showFileCopyDialog(flagType:String) {
        dialog = Dialog(this, R.style.Theme_MaterialComponents_DayNight_Dialog_MinWidth)
        dialog.setCancelable(true)
        copyDialog = CopyingFileDialogBinding.inflate(layoutInflater)
        dialog.setContentView(copyDialog.root)
        copyDialog.progressFile.max = 100

        copyDialog.cancelFileProcess.setOnClickListener(View.OnClickListener {
            var myTag="Copy"
            if (flagType == "Unlock")
            {
                myTag="Unlock"
            }
            binder?.stopFileProcessing(myTag)
            dialog.dismiss()
        })

        binder?.copyFileCallBack(object : FileCopyService.FileCopyCallback {

            override fun fileCopyCallBack(progress: Int, mbCount: String, totalItems: String) {
                Log.d(TAG, "fileCopyCallBack: $progress $mbCount $totalItems")
                if (!FileSize.FILE_COPYING) {
                    dialog.dismiss()
                }
                lifecycleScope.launch(Dispatchers.Main) {
                    copyDialog.addingVaultTitle.text = "Adding Files to DVault"
                    copyDialog.progressFile.progress = progress
                    copyDialog.totalElapsed.text = mbCount
                    copyDialog.totalCount.text = totalItems
                    if (mbCount == "Completed") dialog.dismiss()
                }
            }

            override fun fileUnlockingCallBack(progress: Int, mbCount: String, totalItems: String) {
                Log.d(TAG, "fileUnlockingCallBack : $progress $mbCount $totalItems")
                if (!FileSize.UNLOCK_FILE_COPYING) {
                    dialog.dismiss()
                }
                lifecycleScope.launch(Dispatchers.Main) {
                    copyDialog.addingVaultTitle.text = "Unlocking Files "
                    copyDialog.progressFile.progress = progress
                    copyDialog.totalElapsed.text = mbCount
                    copyDialog.totalCount.text = totalItems
                    if (mbCount == "Completed") dialog.dismiss()
                }
            }
        })

        dialog.show()

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

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
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
            binder = service as FileCopyService.LocalBinder
            serviceBinder = binder?.getService()
            serviceBinder?.randomNumberLiveData?.observe(this@ItemsActivity, Observer {
                Log.d(TAG, "onServiceConnected: $it")
            })

        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBinder = null
            Log.d(TAG, "onServiceDisconnected: Called")
        }
    }

    override fun onItemClick(myItemIdsSet: MutableSet<ItemModel>) {
        if (FileSize.OnLongItemClick) {
            binding.plus.visibility = View.GONE
            binding.unlock.visibility = View.VISIBLE
            binding.selectAll.visibility = View.VISIBLE
        } else {
            binding.plus.visibility = View.VISIBLE
            binding.unlock.visibility = View.GONE
            binding.selectAll.visibility = View.GONE
        }
        showSnackBar(myItemIdsSet.toString())
    }

    override fun deleteParticularItem(itemModel: ItemModel) {
        deleteItem(itemModel)
    }

    override fun unlockParticularItem(itemModel: ItemModel) {
        lifecycleScope.async {
            var fromLoc = File(itemModel.itemCurrentPath)
            var toLoc = File(itemModel.itemOriPath)
            fromLoc.renameTo(toLoc)
            var file = File(itemModel.itemCurrentPath)
            if (file.exists()) {
                var isDeleted = file.delete()
                if (isDeleted) viewModel.deleteItem(itemModel)
            }
        }
    }

    fun deleteItem(itemModel: ItemModel) {
        Log.d(TAG, "deleteItem: ${itemModel.toString()}")
        runBlocking {
            try {
                viewModel.deleteItem(itemModel)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, "deleteItem: ${e.message}")
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (binder==null) {
            // Bind to LocalService
            Intent(this, FileCopyService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

}