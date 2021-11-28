package com.talla.dvault

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.internal.Storage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.activities.DashBoardActivity
import com.talla.dvault.activities.PasswordActivity
import com.talla.dvault.databinding.ActivitySplashBinding
import com.talla.dvault.preferences.UserPreferences
import com.talla.dvault.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.lang.Exception
import javax.inject.Inject
import kotlin.math.log

private const val TAG = "SplashActivity"

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    lateinit var resLauncher: ActivityResultLauncher<Intent>
    private var readPermission: Boolean = false
    private var writePermission: Boolean = false
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var gso: GoogleSignInOptions

    @Inject
    lateinit var userPreference: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
           if (result.resultCode==Activity.RESULT_OK){
               if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
                      if (Environment.isExternalStorageManager()){
                          Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                      }else{
                          Toast.makeText(this, "Permission Rejected", Toast.LENGTH_SHORT).show()
                      }
               }else{
                   Log.d(TAG, "onCreate: Line No 65")
               }
           }
        }

        requestPermissions()
    }

    fun isUserSignedIn(): Boolean {
        val googleSigninAccount = GoogleSignIn.getLastSignedInAccount(this)
        return googleSigninAccount != null
    }

    private fun openLoginScreen() {
        logout()
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openDashBoard() {
        runBlocking {
            val res = viewModel.isLockedOrNot()
            if (res) {
                val intent: Intent = Intent(this@SplashActivity, PasswordActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent: Intent = Intent(this@SplashActivity, DashBoardActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Denied Permission")
        alertDialogBuilder.setMessage("Please allow Storage permission to continue with this app.Because this app works on photos,videos,audios,docs that needs storage permission.")
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setIcon(R.drawable.warning_icon)
        alertDialogBuilder.setPositiveButton("Ok") { dialogInterface, i ->
            dialogInterface.dismiss()
            requestPermissions()
        }.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
            finish()
        }
        alertDialogBuilder.show()
    }


    private fun hasExternalStoragePermission(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            return Environment.isExternalStorageManager()
        }else{
            val hasWritePermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val hasReadPermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            readPermission=hasReadPermissions
            writePermission=hasWritePermissions
            return readPermission && writePermission
        }

//        readPermission = hasReadPermissions
//        writePermission = hasWritePermissions || minSdk29
//

//        return readPermission && (writePermission || minSdk29)
    }

    private fun requestPermissions() {

        if (hasExternalStoragePermission()) {
            runBlocking {
                val res = viewModel.isLoggedInPerfectly()
                Log.d(TAG, "IsLoggedIn Result: $res")
                if (res >= 4 && isUserSignedIn()) {
                    Log.d(TAG, "IsLoggedIn Perfectly: $res")
                    openDashBoard()
                } else {
                    Log.d(TAG, "requestPermissions: Logout")
                    openLoginScreen()
                }
            }
        } else if (!hasExternalStoragePermission()) {


            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R)
            {
                try {
                    val intent=Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    val uri=Uri.fromParts("package:",packageName,null)
                    intent.data = uri
                    resLauncher.launch(intent)
                }catch (e:Exception){
                    e.printStackTrace()
                    val intent=Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    resLauncher.launch(intent)
                }
            }else{
                if (writePermission && readPermission) {
                    val permissionToRequest = mutableListOf<String>()
                    if (!readPermission) {
                        permissionToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    if (!writePermission) {
                        permissionToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
//                    if (permissionToRequest.isNotEmpty()) {
//                        permissionLauncher.launch(permissionToRequest.toTypedArray())
//                    }
                    ActivityCompat.requestPermissions(this, permissionToRequest.toTypedArray(), 0)
                }
            }

        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty()) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission Accepted ${grantResults[i]}")
                    requestPermissions()
                } else {

                    val permission = ActivityCompat.shouldShowRequestPermissionRationale(
                        this@SplashActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    if (permission) {
                        Log.d(TAG, "onRequestPermissionsResult: Should Show Permission Rationale")
                        lifecycleScope.launch {
                            userPreference.saveBooleanData(UserPreferences.FIRST_TIME, true)
                        }
                        showDialog()
                    } else {
                        Log.d(TAG, "onRequestPermissionsResult: Dont Ask Again")
                        lifecycleScope.launch(Dispatchers.Main) {
                            val res: Unit =
                                userPreference.getBooleanData(UserPreferences.FIRST_TIME).collect {
                                    Log.d(TAG, "onRequestPermissionsResult: ${it}")
                                    if (!it) {
                                        requestPermissions()
                                    } else {
                                        val builder: MaterialAlertDialogBuilder =
                                            MaterialAlertDialogBuilder(
                                                this@SplashActivity,
                                                R.style.Theme_MaterialComponents_Light_Dialog_MinWidth
                                            )
                                        builder.setTitle("Requires Permission")
                                        builder.setMessage(
                                            "You have Denied permissions if you want to use this Application " +
                                                    "You have to allow permissions in Settings manually."
                                        )
                                        builder.setCancelable(false)
                                        builder.setIcon(R.drawable.warning_icon)
                                        builder.setPositiveButton(
                                            "Grant"
                                        ) { dialog, which ->
                                            dialog.cancel()
                                            val i = Intent()
                                            i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            i.addCategory(Intent.CATEGORY_DEFAULT)
                                            i.data = Uri.parse("package:$packageName")
                                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//                                            launchIntent.launch(i)
                                            startActivity(i)
                                            finish()
                                        }
                                        builder.setNegativeButton(
                                            "Cancel"
                                        ) { dialog, which ->
                                            dialog.cancel()
                                            finish()
                                        }
                                        builder.show()
                                    }
                                }

                        }
                    }

                }
            }
        } else {
            requestPermissions()
        }
    }

    fun logout() {
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mGoogleSignInClient?.signOut()
            ?.addOnCompleteListener(
                this
            ) { }
    }

}