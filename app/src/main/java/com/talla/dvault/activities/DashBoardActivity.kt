package com.talla.dvault.activities

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Contacts.Intents.Insert.ACTION
import android.provider.ContactsContract.Intents.Insert.ACTION
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import com.talla.dvault.R
import com.talla.dvault.databinding.ActivityDashBoardBinding
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.RequestManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.MainActivity
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.User
import com.talla.dvault.databinding.CustomDialogProfileBinding
import com.talla.dvault.preferences.UserPreferences
import com.talla.dvault.services.FileCopyService
import com.talla.dvault.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import android.app.ActivityManager
import android.content.*
import android.os.IBinder
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.talla.dvault.database.VaultDatabase
import com.talla.dvault.databinding.CustonProgressDialogBinding
import com.talla.dvault.services.DashBoardService
import com.talla.dvault.utills.FileSize
import com.talla.dvault.utills.InternetUtil
import kotlinx.coroutines.*
import java.io.File


private const val TAG = "DashBoardActivity"

@AndroidEntryPoint
class DashBoardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashBoardBinding

    @Inject
    lateinit var appSettingsPrefs: UserPreferences

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var gso: GoogleSignInOptions
    private lateinit var dialog: Dialog
    private lateinit var user: User
    private lateinit var customDialogProfileBinding: CustomDialogProfileBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val viewModel: MainViewModel by viewModels()
    var isNightMode = false
    private lateinit var progressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dialogIninit()
        if (InternetUtil.isInternetAvail(this)) {
            defaultCall()
        } else {
            checkInternetDialog()
        }


        viewModel.getLiveData().observe(this, Observer {
            it?.let {
                it.forEach { catModel ->
                    if (catModel.catId == "Img") binding.totalImages.text =
                        catModel.totalItems.toString()
                    if (catModel.catId == "Vdo") binding.totalVIdeos.text =
                        catModel.totalItems.toString()
                    if (catModel.catId == "Doc") binding.totalDocs.text =
                        catModel.totalItems.toString()
                    if (catModel.catId == "Aud") binding.totalAudios.text =
                        catModel.totalItems.toString()
                }
            }
        })

        lifecycleScope.launch(Dispatchers.IO) {
            appSettingsPrefs.getBooleanData(UserPreferences.NIGHT_MODE).collect { value ->
                withContext(Dispatchers.Main) {
                    if (value) {
                        isNightMode = value
                    } else {
                        isNightMode = false
                    }
                    if (isNightMode) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        checkDarkMode()
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        checkDarkMode()
                    }
                }
            }
        }


        binding.linearLayout.setOnClickListener {
            if (isNightMode) {
                lifecycleScope.launch(Dispatchers.Default) {
                    appSettingsPrefs.saveBooleanData(UserPreferences.NIGHT_MODE, false)
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                lifecycleScope.launch(Dispatchers.Default) {
                    appSettingsPrefs.saveBooleanData(UserPreferences.NIGHT_MODE, true)
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

        }

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Log.d(TAG, "Permission: Granted")
                } else {
                    var result = ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    Log.d(TAG, "Permission: Denied ${result}")
                }
            }

        binding.settingsBtn.setOnClickListener {
            val intent: Intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.audioSelection.setOnClickListener {
            val intent: Intent = Intent(this, FoldersActivity::class.java)
            intent.putExtra(getString(R.string.cat_key), "Aud")
            startActivity(intent)
        }
        binding.docSelection.setOnClickListener {
            val intent: Intent = Intent(this, FoldersActivity::class.java)
            intent.putExtra(getString(R.string.cat_key), "Doc")
            startActivity(intent)
        }
        binding.videoSelection.setOnClickListener {
            val intent: Intent = Intent(this, FoldersActivity::class.java)
            intent.putExtra(getString(R.string.cat_key), "Vdo")
            startActivity(intent)
        }
        binding.imageSelection.setOnClickListener {
            val intent: Intent = Intent(this, FoldersActivity::class.java)
            intent.putExtra(getString(R.string.cat_key), "Img")
            startActivity(intent)
        }

    }

    fun defaultCall() {
        lifecycleScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                user = viewModel.getUserObj()
                binding.userName.text = user.userName
                glide.load(user.userImage).into(binding.userProfilePic)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    binding.userProfilePic.clipToOutline = true
                }
            }
        }
    }

    fun checkDarkMode() {
        if (isNightMode) {
            binding.themeBtn.setImageResource(R.drawable.moon)
            Log.d(TAG, "onCreate: Night")
        } else {
            binding.themeBtn.setImageResource(R.drawable.sun)
            Log.d(TAG, "onCreate: Light")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val brightTODark = binding.themeBtn.drawable as AnimatedVectorDrawable
            brightTODark.start()
        } else {
            Log.d(TAG, "checkDarkMode: Below Lollipop Version")
        }

    }

    private fun checkInternetDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Check Internet !")
        alertDialogBuilder.setMessage(getString(R.string.internet_alert))
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton("retry") { dialogInterface, i ->
            if (InternetUtil.isInternetAvail(this)) {
                dialogInterface.dismiss()
                defaultCall()
            } else {
                checkInternetDialog()
            }
        }
        alertDialogBuilder.show()
    }

    private fun showPofileDialog() {
        dialog = Dialog(this, R.style.ThemeOverlay_MaterialComponents_Dialog)
        dialog.setCancelable(true)
        customDialogProfileBinding = CustomDialogProfileBinding.inflate(layoutInflater)
        dialog.setContentView(customDialogProfileBinding.getRoot())
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            customDialogProfileBinding.userProfilePic.clipToOutline = true
        }
        customDialogProfileBinding.userName.text = user.userName
        customDialogProfileBinding.userEmail.text = user.userEmail
        customDialogProfileBinding.lastLoggedin.text = user.userloginTime
        glide.load(user.userImage).into(customDialogProfileBinding.userProfilePic)
        customDialogProfileBinding.login.setOnClickListener(View.OnClickListener {
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            mGoogleSignInClient?.signOut()
                ?.addOnCompleteListener(this, object : OnCompleteListener<Void> {
                    override fun onComplete(task: Task<Void>) {
                        if (task.isSuccessful) {
                            Log.d(TAG, "onComplete: ${task.result}")
                            dialog.dismiss()
                            showLogOutAlert()
                        } else {
                            Log.d(TAG, "onComplete: ${task.result}")
                            dialog.dismiss()
                            FileSize.showSnackBar("Error Occured retry", binding.root)
                        }

                    }
                })
        })
    }

    fun profileRoot(view: android.view.View) {
        showPofileDialog()
    }

    private fun openIntent() {
        Toast.makeText(this@DashBoardActivity, "Signed Out", Toast.LENGTH_SHORT).show()
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun View.showSnackbar(
        view: View,
        msg: String,
        length: Int,
        actionMessage: CharSequence?,
        action: (View) -> Unit
    ) {
        val snackbar = Snackbar.make(view, msg, length)
        if (actionMessage != null) {
            snackbar.setAction(actionMessage) {
                action(this)
            }.show()
        } else {
            snackbar.show()
        }
    }

    fun dialogIninit() {
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


    private fun showLogOutAlert() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Logout Alert!")
        alertDialogBuilder.setMessage(getString(R.string.logout_alert))
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton("Ok") { dialogInterface, i ->
            dialogInterface.dismiss()
            oldDataDelete()
        }.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        alertDialogBuilder.show()
    }

    private fun oldDataDelete() {
        runBlocking {
            progressDialog.show()
            var deleteJob = lifecycleScope.launch(Dispatchers.Default) {
                appSettingsPrefs.storeStringData(
                    UserPreferences.LAST_BACKUP_TIME,
                    "No BackUp found"
                )
                var pathsArray = arrayListOf<String>(
                    "app_Img",
                    "app_New Folder",
                    "app_Vdo",
                    "app_Aud",
                    "app_Doc"
                )
                pathsArray.forEach {
                    val basePath =
                        this@DashBoardActivity.resources.getString(R.string.db_folders_path)
                    Log.d(TAG, "showDataDeleteDialog: Directory $basePath")
                    val to = File("$basePath$it")
                    to.deleteRecursively()
                }
                viewModel.deletAllAppData()
            }
            deleteJob.join()
            progressDialog.dismiss()
            openIntent()
        }
    }

}