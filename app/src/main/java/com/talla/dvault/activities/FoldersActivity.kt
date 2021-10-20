package com.talla.dvault.activities


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import com.talla.dvault.databinding.ActivityFoldersBinding
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.R
import com.talla.dvault.adapters.FoldersAdapter
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.databinding.FolderBottomSheetBinding
import com.talla.dvault.interfaces.FolderItemClick
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.viewmodels.AppLockViewModel
import com.talla.dvault.viewmodels.FoldersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

private const val TAG = "FoldersActivity"
@AndroidEntryPoint
class FoldersActivity : AppCompatActivity() ,FolderItemClick {
    private lateinit var binding: ActivityFoldersBinding
    private lateinit var foldersAdapter:FoldersAdapter
    private var catType: String? = null
    private var folderId:Int=0
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

        binding.apply {
            createFolder.setOnClickListener {
                showBottomSheetDialog(getString(R.string.create_folder),getString(R.string.create),"New Folder")
            }
            foldersAdapter=FoldersAdapter(catType.toString(),this@FoldersActivity,this@FoldersActivity)
            foldersRCV.adapter=foldersAdapter
        }

        viewModel.getFoldersData(catType.toString()).observe(this, Observer {
            if (it.isEmpty())
            {
                binding.nofolderFound.visibility=View.VISIBLE
                binding.foldersRCV.visibility=View.GONE
            }else{
                binding.nofolderFound.visibility=View.GONE
                binding.foldersRCV.visibility=View.VISIBLE
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

    fun showBottomSheetDialog(title:String,btnText:String,value:String) {
        var bsd = BottomSheetDialog(this, R.style.bottomSheetStyle)
        var bottomView: View? = null
        var sheetBinding: FolderBottomSheetBinding? = null
        sheetBinding= FolderBottomSheetBinding.inflate(layoutInflater)
        bottomView=sheetBinding.root
        bsd.setContentView(bottomView)
        bsd.setCanceledOnTouchOutside(true)
        sheetBinding.apply {
            t1.text=title
            et1.setText(value)
            b1.text=btnText

            b1.setOnClickListener {
                var folderName=et1.text.toString().trim()
                var createdTime=DateUtills.getSystemTime(this@FoldersActivity)
                var catType=catType
                var foldertable=FolderTable(folderName = folderName,folderCreatedAt = createdTime.toString(),folderCatType = catType.toString())
                runBlocking {
                    var btnType=b1.text.toString()
                    if (btnType.equals(this@FoldersActivity.resources.getString(R.string.create)))
                    {
                        viewModel.createNewFolder(foldertable)
                    }else{
                        viewModel.renameFolder(folderName,folderId)
                    }

                }
                bsd.dismiss()
            }

        }


        bsd.show()
    }

    override fun onMenuItemClick(oldFolderName:String,key:String,folderId:Int)
    {
        this.folderId=folderId
        when(key){
            "Rename" -> {
                showBottomSheetDialog(getString(R.string.rename_folder),getString(R.string.update),oldFolderName)
            }
            "Delete" -> {
                runBlocking {
                    viewModel.deleteFolder(folderId)
                }
            }
        }

    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

}