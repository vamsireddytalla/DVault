package com.talla.dvault

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.talla.dvault.activities.DashBoardActivity
import com.talla.dvault.database.VaultDatabase
import com.talla.dvault.database.entities.User
import com.talla.dvault.databinding.ActivityMainBinding
import com.talla.dvault.databinding.CustonProgressDialogBinding
import com.talla.dvault.preferences.UserPreferences
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.utills.FileSize
import com.talla.dvault.utills.InternetUtil
import com.talla.dvault.viewmodels.MainViewModel
import dagger.Provides
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var launchIntent: ActivityResultLauncher<Intent>
    private val viewModel: MainViewModel by viewModels()
    private lateinit var progressDialog: Dialog

    @Inject
    lateinit var databaseInstance: VaultDatabase

    @Inject
    lateinit var appSettingsPrefs: UserPreferences

    @Inject
    lateinit var gso: GoogleSignInOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!isUserSignedIn()) {
            Handler().postDelayed({
                binding.motion1.transitionToEnd()
            }, 1000)
        }
        dialogInit()


        launchIntent =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    handleSignData(result.data)
                } else {
                    Log.d(TAG, "googleSignIn error: ${result.data.toString()}")
                    showSnackBar("Cancelled")
                }
            }

    }


    private fun openIntent() {
        Log.d(TAG, "openIntent: Called")
        val intent: Intent = Intent(this, DashBoardActivity::class.java)
//        intent.flags= Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    fun googleSIgnIn(view: android.view.View) {
        if (InternetUtil.isInternetAvail(this)) {
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            val signInIntent: Intent = mGoogleSignInClient.signInIntent
            launchIntent.launch(signInIntent)
        } else {
            showSnackBar("Check Internet Connection")
        }
    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }


    fun isUserSignedIn(): Boolean {
        val googleSigninAccount = GoogleSignIn.getLastSignedInAccount(this)
        return googleSigninAccount != null
    }


    private fun handleSignData(data: Intent?) {
        // The Task returned from this call is always completed, no need to attach
        // a listener.
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    // user successfully logged-in
                    Log.d(
                        TAG,
                        "handleSignData: ${it.result?.account}  ${it.result?.displayName} ${it.result?.email}"
                    )
                    Log.d(TAG, "handleSignData: ${it.result?.grantedScopes}")
                    Log.d(TAG, "handleSignData: ${it.result?.requestedScopes}")
                    if (it.result?.grantedScopes!!.contains(Scope(DriveScopes.DRIVE_APPDATA))) {
                        var job: Job = lifecycleScope.launch {
                            Log.d(TAG, "handleSignData: ---------->  Granted Scope")
                            var userName = it.result?.displayName
                            var userEmail = it.result?.email
                            var userProfilePic: Uri? = it.result?.photoUrl
                            var currentDT: String? = DateUtills.getSystemTime(this@MainActivity)
                            val user = User(
                                userName.toString(),
                                userEmail.toString(),
                                userProfilePic.toString(),
                                currentDT.toString(),
                                "DVault"
                            )
                            try {
//                                var checkerJob = lifecycleScope.launch(Dispatchers.IO) {
//                                    getDriveFiles(user)
//                                }
                                var res=viewModel.insertData(user)
                                if (res>0) openIntent() else logout()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                FileSize.showSnackBar(e.message.toString(), binding.root)
                            }

                        }

                    } else {
                        Log.d(TAG, "handleSignData: ---------->  Not Granted Scope")
                        showAccessPermission()
                    }


                } else {
                    Log.d(TAG, "handleSignData: ${it.exception.toString()}")
                    Toast.makeText(
                        this,
                        "Error Occured $it.exception.toString()",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener {
                Log.d(TAG, "handleSignData: ${it.localizedMessage}")
                Toast.makeText(this, "Failure ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    fun dialogInit() {
        progressDialog = Dialog(this)
        val customProgressDialogBinding = CustonProgressDialogBinding.inflate(this.layoutInflater)
        progressDialog.setContentView(customProgressDialogBinding.root)
        progressDialog.setCancelable(false)
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

    suspend fun getDriveFiles(user: User) {
        showProgressDialog()
        getDriveService()?.let { gdService ->
            var pagetoken: String? = null
            do {
                val result = gdService.files().list().apply {
                    spaces = "appDataFolder"
                    fields = "nextPageToken, files(id, name)"
                    pageToken = this.pageToken
                }.execute()

                result?.let { res ->
                    if (res.files.isEmpty()) {
                        Log.d(TAG, "getDriveFiles: New User")
                        FileSize.showSnackBar("Welcome !", binding.root)

                    } else {
                        Log.d(TAG, "getDriveFiles: Old User")
                        FileSize.showSnackBar("Welcome Back!", binding.root)
                        var res = viewModel.insertData(user)
                        Log.d(TAG, "getDriveFiles: After User Data Insertion $res")
                        if (res > 0) openIntent() else {
                            logout()
                            showSnackBar("Error occured retry")
                        }
                    }
                }

            } while (pagetoken != null)
        }
        stopProgressDialog()
    }

    private fun showAccessPermission() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("DVault Requires Permission")
        alertDialogBuilder.setMessage("Please allow Edit Access(Check Box) while Login to Save your files to your Drive (Mandatory to use this App).")
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton("Ok") { dialogInterface, i ->
            dialogInterface.dismiss()
            googleSIgnIn(binding.root)
        }.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        alertDialogBuilder.show()
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

    fun logout() {
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mGoogleSignInClient?.signOut()
            ?.addOnCompleteListener(
                this
            ) { }
    }

}