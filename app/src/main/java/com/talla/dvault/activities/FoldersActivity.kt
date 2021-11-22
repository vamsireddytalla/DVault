package com.talla.dvault.activities


import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.R
import com.talla.dvault.adapters.FoldersAdapter
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.databinding.*
import com.talla.dvault.interfaces.FolderItemClick
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.utills.FileSize
import com.talla.dvault.utills.InternetUtil
import com.talla.dvault.viewmodels.AppLockViewModel
import com.talla.dvault.viewmodels.FoldersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import android.os.Environment
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.IOException

private const val TAG = "FoldersActivity"

@AndroidEntryPoint
class FoldersActivity : AppCompatActivity(), FolderItemClick {
    private lateinit var binding: ActivityFoldersBinding
    private lateinit var foldersAdapter: FoldersAdapter
    private var catType: String? = null
    private var folderId: Int = 0
    private lateinit var progressDialog: Dialog
    private lateinit var cloudDialogBinding:CloudLoadingBinding
    private lateinit var dialog: Dialog
    private lateinit var deleteDialogBinding: DeleteDialogBinding
    private val viewModel: FoldersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            catType = bundle.getString("CatKey")
            changeFolderColor(catType!!)
        }
        dialogInit()

        binding.apply {
            createFolder.setOnClickListener {
                showBottomSheetDialog(
                    getString(R.string.create_folder),
                    getString(R.string.create),
                    "New Folder"
                )
            }
            foldersAdapter =
                FoldersAdapter(catType.toString(), this@FoldersActivity, this@FoldersActivity)
            foldersRCV.adapter = foldersAdapter
        }

        viewModel.getFoldersData(catType.toString()).observe(this, Observer {
            if (it.isEmpty()) {
                binding.nofolderFound.visibility = View.VISIBLE
                binding.foldersRCV.visibility = View.GONE
            } else {
                binding.nofolderFound.visibility = View.GONE
                binding.foldersRCV.visibility = View.VISIBLE
                foldersAdapter.differ.submitList(it)
            }

        })

    }

    fun backBtn(view: android.view.View) {
        finish()
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
            DrawableCompat.wrap(binding.createFolder.background),
            ContextCompat.getColor(this, selectedColor!!)
        )
    }

    fun showBottomSheetDialog(title: String, btnText: String, value: String) {
        val bsd = BottomSheetDialog(this, R.style.BottomSheetDialogStyle)
        var bottomView: View? = null
        var sheetBinding: FolderBottomSheetBinding? = null
        sheetBinding = FolderBottomSheetBinding.inflate(layoutInflater)
        bottomView = sheetBinding.root
        bsd.setContentView(bottomView)
        bsd.setCanceledOnTouchOutside(true)
        bsd.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        sheetBinding.apply {
            t1.text = title
            et1.setText(value)
            b1.text = btnText

            b1.setOnClickListener {
                val folderName = et1.text.toString().trim()
                val createdTime = System.currentTimeMillis().toString()
                val catType = catType
                if (folderName.isNotEmpty()) {
                    runBlocking {
                        val btnType = b1.text.toString()
                        val folderModel =
                            FolderTable(0, folderName, createdTime, catType.toString(), "", false)
                        if (btnType == this@FoldersActivity.resources.getString(R.string.create)) {
                            val res = viewModel.createNewFolder(folderModel)
//                        var res: Long =viewModel.checkDataANdCreateFolder(folderName,createdTime.toString(),catType.toString())
                            if (res == 2067L) {
                                Toast.makeText(
                                    this@FoldersActivity,
                                    getString(R.string.already_existed),
                                    Toast.LENGTH_SHORT
                                ).show()
                                et1.error = getString(R.string.already_existed)
                                et1.requestFocus()
                            } else {
                                createFolder(catType.toString(), folderName)
                                bsd.dismiss()
                                showSnackBar("Created")
                            }
                            Log.d(TAG, "showBottomSheetDialog: ${res}")
                        } else {
                            if (InternetUtil.isInternetAvail(this@FoldersActivity)) {
                               showProgressDialog()
                                val res: Int = viewModel.renameFolder(folderName, folderId)
                                if (res == 2067) {
                                    Toast.makeText(
                                        this@FoldersActivity,
                                        getString(R.string.already_existed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    et1.error = getString(R.string.already_existed)
                                    et1.requestFocus()
                                    stopProgressDialog()
                                } else {
                                    updateFolder(value, folderName)
                                    bsd.dismiss()
                                    val folderObj =
                                        viewModel.getFolderObjWithFolderID(folderId.toString())
                                    if (folderObj.folderServerId.isNotEmpty()) updateFolderName(
                                        folderObj
                                    )
                                    showSnackBar("Updated Successfully!")
                                   stopProgressDialog()
                                }
                            } else {
                                Toast.makeText(
                                    this@FoldersActivity,
                                    "Check Internet!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    }
                } else {
                    et1.error = "empty"
                    et1.requestFocus()
                }

            }

        }

        bsd.show()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val view = currentFocus
        if (view != null && (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_MOVE) && view is EditText && !view.javaClass.name.startsWith(
                "android.webkit."
            )
        ) {
            val scrcoords = IntArray(2)
            view.getLocationOnScreen(scrcoords)
            val x = ev.rawX + view.getLeft() - scrcoords[0]
            val y = ev.rawY + view.getTop() - scrcoords[1]
            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom()) (this.getSystemService(
                INPUT_METHOD_SERVICE
            ) as InputMethodManager).hideSoftInputFromWindow(
                this.window.decorView.applicationWindowToken, 0
            )
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onMenuItemClick(oldFolderName: String, key: String, folderId: Int) {
        this.folderId = folderId
        when (key) {
            "Rename" -> {
                showBottomSheetDialog(
                    getString(R.string.rename_folder),
                    getString(R.string.update),
                    oldFolderName
                )
            }
            "Delete" -> {
                val res = FileSize.checkIsAnyProcessGoing()
                if (res.isNotEmpty()) {
                    showSnackBar(res)
                } else {
                    //process continue here
                    showDeleteDialog(oldFolderName, folderId.toString())
                }
            }
        }

    }

    private fun showDeleteDialog(itemName: String, folderId: String) {
        dialog = Dialog(this, R.style.Theme_MaterialComponents_DayNight_Dialog_MinWidth)
        dialog.setCancelable(true)
        deleteDialogBinding = DeleteDialogBinding.inflate(layoutInflater)
        dialog.setContentView(deleteDialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        deleteDialogBinding.fileName.text = itemName
        //online Delete and local delete
        deleteDialogBinding.yes.setOnClickListener {
            val fileProcess = FileSize.checkIsAnyProcessGoing()
            if (fileProcess.isEmpty()) {
                runBlocking {
                    dialog.dismiss()
                   showProgressDialog()
                    val folderObj = viewModel.getFolderObjWithFolderID(folderId)
                    if (deleteDialogBinding.isServDel.isChecked) {
                        Log.d(TAG, "showDeleteDialog: Server Delete")
                        checkedServDelete(folderObj)
                    } else {
                        Log.d(TAG, "showDeleteDialog: Local Delete")
                        localFileDelete(folderObj)
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

    suspend fun checkedServDelete(folderObj: FolderTable) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (InternetUtil.isInternetAvail(this@FoldersActivity)) {
                val eeee = lifecycleScope.launch(Dispatchers.IO) {
                    if (folderObj.folderServerId.isNotEmpty()) {
                        onlineFileDelete(folderObj)
                    }
                }
                eeee.join()
                val newFolderObj = viewModel.getFolderObjWithFolderID(folderObj.folderId.toString())
                localFileDelete(newFolderObj)
            } else {
                showSnackBar("Check Internet Connection")
            }
        }
    }

    suspend fun localFileDelete(folderTable: FolderTable) {
        lifecycleScope.launch(Dispatchers.IO) {
            val orgDir = this@FoldersActivity.resources.getString(R.string.db_folder_path)
            val sourceFile =
                File(orgDir.toString() + "/" + "app_" + folderTable.folderCatType + "/" + folderTable.folderName)
            Log.d(TAG, "localFileDelete: File Path --> ${sourceFile.toString()}")
            if (sourceFile.exists()) {
                val isDeleted = sourceFile.deleteRecursively()
            }
            if (folderTable.folderServerId.isEmpty()) {
                Log.d(TAG, "localFileDelete: Folder Delete Locally Called")
                viewModel.deleteFolder(folderId = folderId.toInt())
            } else {
                withContext(Dispatchers.Main) {
                    foldersAdapter.notifyDataSetChanged()
                }
            }
        }
       stopProgressDialog()
    }

    suspend fun onlineFileDelete(folderObj: FolderTable) {
        val files = getDriveService()?.let {
            it.files().delete(folderObj.folderServerId).execute()
        }
        viewModel.updateFolderServIdBasedOnFolderId(folderObj.folderId.toString(), "")
    }

    fun getItemsBasedOnFolderId(folderId: String): List<ItemModel> {
        var itemsListOnFolderId = ArrayList<ItemModel>()
        runBlocking {
            itemsListOnFolderId =
                viewModel.getItemsBasedOnFolderId(folderId) as ArrayList<ItemModel>
        }
        return itemsListOnFolderId
    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
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

    fun createFolder(catName: String, folderName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newdir: File = this@FoldersActivity.getDir(catName, Context.MODE_PRIVATE)
            val testFolder = File("$newdir/$folderName")
            if (!testFolder.exists()) {
                testFolder.mkdirs()
                Log.d(TAG, "createFolder: Creating")
            }
            Log.d(TAG, "createFolder: ${testFolder.toString()}")
        }
    }

    fun updateFolder(oldFolderName: String, newFolderName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newdir: File = this@FoldersActivity.getDir(catType, Context.MODE_PRIVATE)
            val oldFolder = File("$newdir/$oldFolderName")
            val newFolder = File("$newdir/$newFolderName")
            val success = oldFolder.renameTo(newFolder)
            Log.d(TAG, "updateFolder: $success")
        }
    }

    fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(this)?.let { googleAccount ->
            val credential =
                GoogleAccountCredential.usingOAuth2(this, listOf(DriveScopes.DRIVE_APPDATA))
            credential.selectedAccount = googleAccount.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(this.getString(R.string.app_name))
                .build()
        }
        return null
    }

    suspend fun updateFolderName(folderObj: FolderTable) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val mimeType = this@FoldersActivity.resources.getString(R.string.folder_mime_type)
                // File's new content.
                val newMetadata = com.google.api.services.drive.model.File()
                newMetadata.name = folderObj.folderName

                // Send the request to the API.
                val fileRes =
                    getDriveService()!!.files().update(folderObj.folderServerId, newMetadata)
                        .setFields("id, name, appProperties,quotaBytesUsed").execute()
                fileRes?.let {
                    Log.d(
                        TAG,
                        "updateFolderName: Successfully ${fileRes.name} ${fileRes.id} ${fileRes.quotaBytesUsed}"
                    )
                }
            } catch (e: IOException) {
                println("An error occurred: $e")
                Log.d(TAG, "updateFolderName Error --> : ${e.message}")
            }
        }
    }


}