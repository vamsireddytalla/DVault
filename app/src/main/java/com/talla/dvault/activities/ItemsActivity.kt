package com.talla.dvault.activities

import android.app.Activity
import android.app.ActivityManager
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.RequestManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.talla.dvault.R
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.talla.dvault.adapters.ItemsAdapter
import com.talla.dvault.database.entities.DeleteFilesTable
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.database.entities.SourcesModel
import com.talla.dvault.databinding.*
import com.talla.dvault.interfaces.ItemAdapterClick
import com.talla.dvault.services.FileCopyService
import com.talla.dvault.utills.FileSize
import com.talla.dvault.utills.InternetUtil
import com.talla.dvault.utills.RealPathUtill
import com.talla.dvault.viewmodels.ItemViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import java.io.*
import java.lang.Exception
import javax.xml.transform.Source

private const val TAG = "ItemsActivity"

@AndroidEntryPoint
class ItemsActivity : AppCompatActivity(), ItemAdapterClick {
    private lateinit var binding: ActivityItemsBinding
    private lateinit var folderTable: FolderTable
    private lateinit var itemsAdapter: ItemsAdapter
    private var itemsList: List<ItemModel> = ArrayList()
    private var urisList = mutableListOf<Uri>()
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    //    private var mBound: Boolean = false
    private lateinit var dialog: Dialog
    private var pos: Int? = null
    private lateinit var progressDialog: Dialog
    private lateinit var cloudDialogBinding: CloudLoadingBinding
    private var binder: FileCopyService.LocalBinder? = null
    private var serviceBinder: FileCopyService? = null
    private lateinit var deleteDialogBinding: DeleteDialogBinding
    private lateinit var copyDialog: CopyingFileDialogBinding


    @Inject
    lateinit var glide: RequestManager
    private val viewModel: ItemViewModel by viewModels()

    private fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(this)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                this, listOf(DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = googleAccount.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(getString(R.string.app_name))
                .build()
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            folderTable =
                intent.getSerializableExtra(this.resources.getString(R.string.key)) as FolderTable
            binding.titleScrnTitle.text = folderTable.folderName
            Log.d("FolderName", "onCreate: $folderTable.folderName")
            changeFolderColor(folderTable.folderCatType)
            FileSize.OnLongItemClick = false
            FileSize.SelectAll = false
        }
        dialogInit()

        val res: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val receivedData: Intent? = result.data
                    Log.d(TAG, "onCreate: ${receivedData?.data?.path.toString()}")
                    receivedData?.let {
                        val sourceList = ArrayList<SourcesModel>()
                        if (null != receivedData.clipData) {
                            for (i in 0 until receivedData.clipData!!.itemCount) {
                                val uri = receivedData.clipData!!.getItemAt(i).uri
                                val fileRealPath: String? = RealPathUtill.getRealPath(this, uri!!)
                                val file = File(fileRealPath)
                                val filesize: Long = file.length()
                                val fileMimeType = FileSize.getMimeType(file.toString()).toString()
                                val sourceModel = SourcesModel(
                                    fileRealPath!!,
                                    file.name,
                                    filesize,
                                    fileMimeType,
                                    folderTable
                                )
                                sourceList.add(sourceModel)
                            }
                        } else {
                            val uri: Uri? = receivedData.data
                            Log.d(TAG, "Selected File path: $uri")
                            val fileRealPath: String? = RealPathUtill.getRealPath(this, uri!!)
                            Log.d(TAG, "Real Converted file path $fileRealPath")
                            val file = File(fileRealPath)
                            val filesize: Long = file.length()
                            val fileMimeType = FileSize.getMimeType(file.toString()).toString()
                            val sourceModel = SourcesModel(
                                fileRealPath!!,
                                file.name,
                                filesize,
                                fileMimeType,
                                folderTable
                            )
                            sourceList.add(sourceModel)
                        }

                        binder?.startFileCopyingService(sourceList)
                        showFileCopyDialog("Copy")

                    } ?: showSnackBar("No File Selected !")
                } else {
                    Log.d(TAG, "onCreate: No Items Selected")
                }
            }

        val sdk30ImgVdoRes =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                if (res.resultCode == Activity.RESULT_OK) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        val resData: Intent? = res.data
                        val sourceList = ArrayList<SourcesModel>()
                        resData?.let { resultData ->
                            Log.d(TAG, "onCreate: ${resultData.clipData}")
                            resultData.clipData?.let { clip ->
                                for (i in 0 until clip.itemCount) {
                                    val uri = clip.getItemAt(i).uri
                                    Log.d(TAG, "Multiple uris : $uri")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        val sourceModel = sdk30Converters(uri)
                                        sourceModel?.let { sourceList.add(it) }
                                    }
                                }
                                Log.d(TAG, "Multiple Items: ${sourceList.toString()}")
                                if (sourceList.isNotEmpty()) {
                                    binder?.startFileCopyingService(sourceList)
                                    withContext(Dispatchers.Main) {
                                        showFileCopyDialog("Copy")
                                    }
                                }
                            }

                        } ?: showSnackBar("No File Selected !")
                    }

                }
            }

        intentSenderLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == RESULT_OK) {
                    lifecycleScope.launch { viewModel.deleteBasedOnCat(folderTable.folderCatType) }
                    showSnackBar(getString(R.string.unwanted_files))
                } else {
                    showSnackBar(getString(R.string.error_occured))
                }
            }

        binding.plus.setOnClickListener {
            if (!FileSize.FILE_COPYING && !FileSize.UNLOCK_FILE_COPYING) {


                if (FileSize.checkVersion30()) {
                    when (folderTable.folderCatType) {
                        "Img" -> {
                            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            val pickIntent = Intent(Intent.ACTION_PICK, collection)
                            pickIntent.type = "image/*"
                            pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            sdk30ImgVdoRes.launch(Intent.createChooser(pickIntent, "Select Images"))
                        }
                        "Vdo" -> {
                            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            val pickIntent = Intent(Intent.ACTION_PICK, collection)
                            pickIntent.type = "video/*"
                            pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            sdk30ImgVdoRes.launch(Intent.createChooser(pickIntent, "Select Videos"))
                        }
                        "Aud" -> {
                            Intent(this, CustomFilePickerActivity::class.java).apply {
                                this.putExtra(resources.getString(R.string.custom_key), "Audios")
                                startActivity(this)
                            }
                        }
                        "Doc" -> {
                            showSnackBar("Docs functionality Comming Soon")
                        }
                    }

                } else {
                    val openFileIntent = Intent()
                    when (folderTable.folderCatType) {
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
                    openFileIntent.action = Intent.ACTION_GET_CONTENT
                    res.launch(Intent.createChooser(openFileIntent, "Select Multiple Items"))
                }


            } else {
                showSnackBar("Already files in Processing...")
                binder?.let { showFileCopyDialog("Copy") }
            }

        }

        binding.apply {
            itemsAdapter =
                ItemsAdapter(this@ItemsActivity, itemsList, glide, this@ItemsActivity, folderTable)
            itemRCV.adapter = itemsAdapter

            backbtn.setOnClickListener {
                finish()
            }

        }

        viewModel.getItemsBasedOnCatType(folderTable.folderCatType, folderTable.folderId)
            .observe(this, Observer {
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

        viewModel.getDeleteItemsOnCategory(folderTable.folderCatType).observe(this) {
            if (it.isNotEmpty()) {
                for (i in it){
                 urisList.add(i.toUri())
                }
               if (FileSize.selectedCustomItems.isNotEmpty())  checkIsAnyFileDelete()
            }
        }

        binding.selectAll.setOnClickListener {
            selectALlCall()
        }

        binding.unlock.setOnClickListener {
            if (FileSize.selectedUnlockItems.isNotEmpty() && !FileSize.UNLOCK_FILE_COPYING && !FileSize.FILE_COPYING) {
                binder?.unlockFilesService(FileSize.selectedUnlockItems)
                FileSize.SelectAll = true
                selectALlCall()
                binding.unlock.visibility = View.GONE
                binding.selectAll.visibility = View.GONE
                showFileCopyDialog("Unlock")
            } else {
                showSnackBar("Unlocking files already in Processing...")
                binder?.let { showFileCopyDialog("Unlock") }
            }
        }

    }

    private fun checkIsAnyFileDelete() {
        if (FileSize.checkVersion30()) {
            when(folderTable.folderCatType){
                getString(R.string.Img) -> showSdk30DeleteDialog("Delete Images",getString(R.string.user_delete_description))
                getString(R.string.Vdo) -> showSdk30DeleteDialog("Delete Videos",getString(R.string.user_delete_description))
                getString(R.string.Aud) -> showSdk30DeleteDialog("Delete Audios",getString(R.string.user_delete_description))
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun sdk30Converters(uri: Uri): SourcesModel? {
        var sourceModel: SourcesModel? = null
        Log.d(TAG, "sdk30Converters: ${folderTable.folderCatType}")
        if (folderTable.folderCatType == getString(R.string.Img)) {
            sourceModel = FileSize.imageConvertData(uri, this@ItemsActivity, folderTable)
            Log.d(TAG, "image called ${sourceModel.toString()}")
        } else {
            sourceModel = FileSize.videoConvertData(uri, this@ItemsActivity, folderTable)
            Log.d(TAG, "sdk30Converters: video called ${sourceModel.toString()}")
        }
        return sourceModel
    }

    override fun onResume() {
        super.onResume()
        if (FileSize.checkVersion30()) {
            if (!FileSize.FILE_COPYING && !FileSize.UNLOCK_FILE_COPYING && folderTable.folderCatType == getString(
                    R.string.Aud
                ) && FileSize.selectedCustomItems.isNotEmpty()
            ) {
                val sourceList = ArrayList<SourcesModel>()
                val sourceModelList = FileSize.selectedCustomItems
                for (source in sourceModelList) {
                    sourceList.add(
                        SourcesModel(
                            source.contentUri,
                            source.name,
                            source.size.toLong(),
                            source.mimeType,
                            folderTable
                        )
                    )
                }
                Log.d(TAG, "onResume: ")
                //start service
                binder?.startFileCopyingService(sourceList)
                showFileCopyDialog("Copy")
            }
        }
    }

    fun selectALlCall() {
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
            Log.d(TAG, "Select ALl CLicked MyItems Ids ${FileSize.selectedUnlockItems.toString()}")
        }
    }

    fun startFileCopyService(sourceList: List<SourcesModel>) {
        val fileService = Intent(this@ItemsActivity, FileCopyService::class.java)
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

    private fun showFileCopyDialog(flagType: String) {
        dialog = Dialog(this, R.style.Theme_MaterialComponents_DayNight_Dialog_MinWidth)
        dialog.setCancelable(true)
        copyDialog = CopyingFileDialogBinding.inflate(layoutInflater)
        dialog.setContentView(copyDialog.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        copyDialog.progressFile.max = 100

        copyDialog.cancelFileProcess.setOnClickListener(View.OnClickListener {
            var myTag = "Copy"
            if (flagType == "Unlock") {
                myTag = "Unlock"
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
                    copyDialog.addingVaultTitle.text = getString(R.string.adding_files_to_dvault)
                    copyDialog.progressFile.progress = progress
                    copyDialog.totalElapsed.text = mbCount
                    copyDialog.totalCount.text = totalItems
                    if (mbCount == "Completed") {
                        showSnackBar(mbCount)
                        checkIsAnyFileDelete()
                        dialog.dismiss()
                    }
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
                    if (mbCount == "Completed") {
                        itemsAdapter.notifyDataSetChanged()
                        showSnackBar(mbCount)
                        dialog.dismiss()
                    }
                }
            }
        })

        dialog.show()

    }

    fun getFolderNameBasedOnCat(): String {
        var folderName: String? = null
        when (folderTable.folderCatType) {
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
        val catFolderName = getFolderNameBasedOnCat()
        val newdir: File = this.getDir(catFolderName, Context.MODE_PRIVATE)
        val files: Array<File> = newdir.listFiles()
        Log.d("Files", "Size: " + files.size)
        for (i in files.indices) {
            Log.d("Files", "FileName:" + files[i].name)
        }
    }

    fun changeFolderColor(type: String) {
        var selectedColor: Int? = null

        when (type) {
            "Img" -> {
                selectedColor = R.color.light_pink
            }
            "Aud" -> {
                selectedColor = R.color.light_yellow
            }
            "Doc" -> {
                selectedColor = R.color.light_blue
            }
            "Vdo" -> {
                selectedColor = R.color.light_violet
            }
        }
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
//        showSnackBar(myItemIdsSet.toString())
    }

    override fun deleteParticularItem(itemModel: ItemModel, pos: Int) {
//        deleteItem(itemModel)
        this.pos = pos
        showDeleteDialog(itemModel)
    }

    override fun unlockParticularItem(itemModel: ItemModel) {
        lifecycleScope.async {
            val fromLoc = File(itemModel.itemCurrentPath)
            val toLoc = File(itemModel.itemOriPath)
            fromLoc.renameTo(toLoc)
            val file = File(itemModel.itemCurrentPath)
            if (file.exists()) {
                val isDeleted = file.delete()
                if (isDeleted) viewModel.deleteItem(itemModel)
                itemsAdapter.notifyDataSetChanged()
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

        if (binder == null) {
            // Bind to LocalService
            Intent(this, FileCopyService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun showDeleteDialog(itemModel: ItemModel) {
        dialog = Dialog(this, R.style.Theme_MaterialComponents_DayNight_Dialog_MinWidth)
        dialog.setCancelable(true)
        deleteDialogBinding = DeleteDialogBinding.inflate(layoutInflater)
        dialog.setContentView(deleteDialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        deleteDialogBinding.MainHeading.text=getString(R.string.delete_item)
        deleteDialogBinding.fileName.text = itemModel.itemName
        //online Delete and local delete
        deleteDialogBinding.yes.setOnClickListener {

            val fileProcess = FileSize.checkIsAnyProcessGoing()
            if (fileProcess.isEmpty()) {
                runBlocking {
                    dialog.dismiss()
                    showProgressDialog()
                    if (deleteDialogBinding.isServDel.isChecked) {
                        Log.d(TAG, "showDeleteDialog: Server Delete")
                        checkedServDelete(itemModel)
                    } else {
                        Log.d(TAG, "showDeleteDialog: Local Delete")
                        localFileDelete(itemModel, false)
                    }
                }
            } else {
                dialog.dismiss()
                showSnackBar(fileProcess)
            }
        }
        deleteDialogBinding.no.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    suspend fun checkedServDelete(itemModel: ItemModel) {
        if (InternetUtil.isInternetAvail(this)) {
            lifecycleScope.launch(Dispatchers.IO) {
                if (itemModel.serverId.isNotEmpty()) {
                    onlineFileDelete(itemModel.serverId)
                    localFileDelete(itemModel, true)
                } else {
                    localFileDelete(itemModel, false)
                }
            }
        } else {
            stopProgressDialog()
            showSnackBar("Check Internet Connection")
        }
    }

    suspend fun localFileDelete(itemModel: ItemModel, isServerDelete: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val orgDir = this@ItemsActivity.resources.getString(R.string.db_folder_path)
            val sourceFile =
                File(orgDir.toString() + "/" + "app_" + folderTable.folderCatType + "/" + folderTable.folderName + "/" + itemModel.itemName)
            Log.d(TAG, "localFileDelete: File Path --> ${sourceFile.toString()}")
            if (sourceFile.exists()) {
                val isDeleted = sourceFile.delete()
            }
            itemModel.isDeleted = true
            withContext(Dispatchers.Main) {
                if (isServerDelete || itemModel.serverId.isEmpty()) {
                    viewModel.deleteItem(itemModel)
                    Log.d(TAG, "localFileDelete: if called")
                } else {
                    Log.d(TAG, "localFileDelete: Else Called")
                    itemsAdapter.notifyItemChanged(pos!!, itemModel)
                }
                stopProgressDialog()
            }
        }

    }

    fun onlineFileDelete(servId: String) {
        val files = getDriveService()?.let {
            it.files().delete(servId).execute()
        }
    }

    fun dialogInit() {
        progressDialog = Dialog(this)
        cloudDialogBinding = CloudLoadingBinding.inflate(this.layoutInflater)
        progressDialog.setContentView(cloudDialogBinding.root)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.setCancelable(false)
    }

    suspend fun showProgressDialog() {
        val rotationAnimation = AnimationUtils.loadAnimation(this, R.anim.loading_anim)
        cloudDialogBinding.prog.startAnimation(rotationAnimation)
        progressDialog.show()
    }

    suspend fun stopProgressDialog() {
        progressDialog.dismiss()
    }

    fun showSdk30DeleteDialog(title: String, description: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setMessage(description)
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton("Yes") { dialogInterface, i ->
            lifecycleScope.launch { deletePhotoFromExternalStorage() }
            dialogInterface.dismiss()
        }
        alertDialogBuilder.setNegativeButton("No") { dialogInterface, i ->
            dialogInterface.dismiss()
        }
        alertDialogBuilder.show()

    }

    private suspend fun deletePhotoFromExternalStorage() {
        withContext(Dispatchers.IO) {
            try {
                contentResolver.delete(urisList.get(0), null, null)
                Log.d(TAG, "deletePhotoFromExternalStorage: Only try block is executed")
            } catch (e: SecurityException) {
                Log.d(TAG, "deletePhotoFromExternalStorage: Exception Occured")
                val intentSender = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {

                        MediaStore.createDeleteRequest(
                            contentResolver,
                            urisList
                        ).intentSender
                    }
                    else -> null
                }
                intentSender?.let { sender ->
                    intentSenderLauncher.launch(
                        IntentSenderRequest.Builder(sender).build()
                    )
                }
            }
        }
    }

}