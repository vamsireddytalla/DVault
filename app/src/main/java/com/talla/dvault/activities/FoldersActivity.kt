package com.talla.dvault.activities


import android.app.Dialog
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.R
import com.talla.dvault.adapters.FoldersAdapter
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.databinding.*
import com.talla.dvault.interfaces.FolderItemClick
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.utills.FileSize
import com.talla.dvault.viewmodels.AppLockViewModel
import com.talla.dvault.viewmodels.FoldersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TAG = "FoldersActivity"

@AndroidEntryPoint
class FoldersActivity : AppCompatActivity(), FolderItemClick {
    private lateinit var binding: ActivityFoldersBinding
    private lateinit var foldersAdapter: FoldersAdapter
    private var catType: String? = null
    private var folderId: Int = 0
    private lateinit var progressDialog: Dialog
    private lateinit var dialog: Dialog
    private lateinit var deleteDialogBinding: DeleteDialogBinding
    private val viewModel: FoldersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var bundle: Bundle? = intent.extras
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
        var bsd = BottomSheetDialog(this, R.style.BottomSheetDialogStyle)
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
                var folderName = et1.text.toString().trim()
                var createdTime = System.currentTimeMillis().toString()
                var catType = catType
                runBlocking {
                    var btnType = b1.text.toString()
                    val folderModel =
                        FolderTable(0, folderName, createdTime, catType.toString(), "", false)
                    if (btnType == this@FoldersActivity.resources.getString(R.string.create)) {
                        var res = viewModel.createNewFolder(folderModel)
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
                            bsd.dismiss()
                            showSnackBar("Created")
                        }
                        Log.d(TAG, "showBottomSheetDialog: ${res}")
                    } else {
                        var res: Int = viewModel.renameFolder(folderName, folderId)
                        if (res == 2067) {
                            Toast.makeText(
                                this@FoldersActivity,
                                getString(R.string.already_existed),
                                Toast.LENGTH_SHORT
                            ).show()
                            et1.error = getString(R.string.already_existed)
                            et1.requestFocus()
                        } else {
                            bsd.dismiss()
                            showSnackBar("Updated Successfully!")
                        }
                    }

                }

            }

        }


        bsd.show()
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
                runBlocking {
//                    viewModel.deleteFolder(folderId)
                    showDeleteDialog()
                }
            }
        }

    }

    private fun showDeleteDialog() {
        dialog = Dialog(this, R.style.Theme_MaterialComponents_DayNight_Dialog_MinWidth)
        dialog.setCancelable(true)
        deleteDialogBinding = DeleteDialogBinding.inflate(layoutInflater)
        dialog.setContentView(deleteDialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    fun dialogInit() {
        progressDialog = Dialog(this)
        val customProgressDialogBinding = CustonProgressDialogBinding.inflate(this.layoutInflater)
        progressDialog.setContentView(customProgressDialogBinding.root)
        progressDialog.setCancelable(false)
    }

    suspend fun showProgressDialog() {
        withContext(Dispatchers.Main) {
            progressDialog.show()
        }
    }

    suspend fun stopProgressDialog() {
        withContext(Dispatchers.Main) {
            progressDialog.dismiss()
        }
    }

}