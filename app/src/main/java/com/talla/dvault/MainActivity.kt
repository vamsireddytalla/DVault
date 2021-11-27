package com.talla.dvault

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
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
import com.google.api.client.googleapis.media.MediaHttpDownloader
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.talla.dvault.activities.DashBoardActivity
import com.talla.dvault.database.VaultDatabase
import com.talla.dvault.database.entities.CategoriesModel
import com.talla.dvault.database.entities.FolderTable
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.database.entities.User
import com.talla.dvault.databinding.ActivityMainBinding
import com.talla.dvault.databinding.CloudLoadingBinding
import com.talla.dvault.databinding.CustonProgressDialogBinding
import com.talla.dvault.preferences.UserPreferences
import com.talla.dvault.repositories.VaultRepository
import com.talla.dvault.services.DriveService
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.utills.FileSize
import com.talla.dvault.utills.InternetUtil
import com.talla.dvault.viewmodels.MainViewModel
import dagger.Provides
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.*
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList
import kotlin.math.log
import kotlin.system.exitProcess
import android.text.Html

import android.os.Build
import android.text.method.LinkMovementMethod


private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var launchIntent: ActivityResultLauncher<Intent>
    private val viewModel: MainViewModel by viewModels()
    private lateinit var progressDialog: Dialog
    private lateinit var gDriveService: Drive
    private lateinit var cloudDialogBinding:CloudLoadingBinding

    @Inject
    lateinit var databaseInstance: VaultDatabase

    @Inject
    lateinit var appSettingsPrefs: UserPreferences

    @Inject
    lateinit var repository: VaultRepository

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

        binding.privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        binding.privacyPolicy.setLinkTextColor(Color.BLUE)
        binding.privacyPolicy.setOnClickListener{
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(this.resources.getString(R.string.privacy_policy)))
            startActivity(browserIntent)
        }
    }

    private fun openIntent() {
        lifecycleScope.launch {
            val res = viewModel.isLoggedInPerfectly()
            stopProgressDialog()
            if (res >= 4 && isUserSignedIn()) {
                Log.d(TAG, "openIntent: Called")
                val intent: Intent = Intent(this@MainActivity, DashBoardActivity::class.java)
                startActivity(intent)
                finish()
//                exitProcess(0)
            } else {
                FileSize.showSnackBar("Error occured retry", binding.root)
            }
        }
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
                            val userName = it.result?.displayName
                            val userEmail = it.result?.email
                            val userProfilePic: Uri? = it.result?.photoUrl
                            val currentDT: String? = System.currentTimeMillis().toString()
                            val user = User(
                                userName.toString(),
                                userEmail.toString(),
                                userProfilePic.toString(),
                                currentDT.toString(),
                                "DVault"
                            )
                            try {
                                showProgressDialog()
                                lifecycleScope.launch(Dispatchers.Default) {
                                    getCategoriesData(user)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                stopProgressDialog()
                                FileSize.showSnackBar(e.message.toString(), binding.root)
                                logout("handleSignDat())")
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
        cloudDialogBinding = CloudLoadingBinding.inflate(this.layoutInflater)
        progressDialog.setContentView(cloudDialogBinding.root)
        val rotationAnimation = AnimationUtils.loadAnimation(this, R.anim.loading_anim)
        cloudDialogBinding.prog.startAnimation(rotationAnimation)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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

    suspend fun logout(message: String) {
        stopProgressDialog()
        Log.d(TAG, "logout Called From $message")
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mGoogleSignInClient?.signOut()
            ?.addOnCompleteListener(
                this
            ) { }
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
        val rotationAnimation = AnimationUtils.loadAnimation(this, R.anim.loading_anim)
        cloudDialogBinding.prog.startAnimation(rotationAnimation)
        progressDialog.show()
    }

    suspend fun stopProgressDialog() {
            progressDialog.dismiss()
    }

    fun getCategoriesData(user: User) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                getDriveService()?.let { gdFiles ->
                    gDriveService = gdFiles
                    val rootFolderId: String =
                        gdFiles.files().get("appDataFolder").setFields("id").execute().getId()
                    Log.d(TAG, "getCategoriesData: $rootFolderId")
                    val mimetype = this@MainActivity.resources.getString(R.string.folder_mime_type)
                    val result = getDataBasedOnParentId(rootFolderId, mimetype)
                    result?.let { res ->
                        Log.d(TAG, "getDriveFiles Count From Server : ${res.files}")
                        Log.d(TAG, "getDriveFiles: Server Files Count  ${res.files.size}")
                        withContext(Dispatchers.Main) {
                            viewModel.insertData(user)
                        }
                        if (res.files.size < 4) {
                            Log.d(TAG, "getCategoriesData: Sign Up Bad")
                            Log.d(TAG, "getCategoriesData: New User")
                            FileSize.showSnackBar("Welcome !", binding.root)
                            //insert into categories table and get remaining folders to be created on server
                            cacheCatListAndInsertInLocalDB(res)
                        } else {
                            Log.d(TAG, "getCategoriesData: Sign Up Good")
                            Log.d(TAG, "getCategoriesData: Old User")
                            FileSize.showSnackBar("Welcome Back !", binding.root)
                            // get all categories folders and store in room database
                            cacheCatListAndInsertInLocalDB(res)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                FileSize.showSnackBar(e.message.toString(), binding.root)
                Log.d(TAG, "getCategoriesData: Exception Occured -> ${e.message}")
                logout("getCategoriesData()")
            }
        }

    }

    suspend fun cacheCatListAndInsertInLocalDB(res: FileList) {
        Log.d(TAG, "cacheCatListAndInsertInLocalDB: Called")
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                val catList = ArrayList<CategoriesModel>()
                if (res.files.isNotEmpty()) {
                    res.files.forEach { fileee ->
                        val driveTimeStamp =
                            DateUtills.driveDateToTimeStamp(fileee.createdTime.toString())
                        Log.d(TAG, "cacheCatListAndInsertInLocalDB: $driveTimeStamp")
                        Log.d(
                            TAG,
                            "cacheCatListAndInsertInLocalDB: ${
                                DateUtills.converTimeStampToDate(
                                    this@MainActivity,
                                    driveTimeStamp
                                )
                            }"
                        )
                        Log.d(
                            TAG,
                            "cacheCatListAndInsertInLocalDB: Server File Sizes ${
                                FileSize.bytesToHuman(fileee.quotaBytesUsed)
                            } ${fileee.name}"
                        )
                        var name = ""
                        when (fileee.name) {
                            "Img" -> name = "Images"
                            "Aud" -> name = "Audios"
                            "Vdo" -> name = "Videos"
                            "Doc" -> name = "Docs"
                        }
                        val catModel = CategoriesModel(
                            fileee.name,
                            name,
                            fileee.mimeType,
                            fileee.id,
                            fileee.parents.get(0)
                        )
                        catList.add(catModel)
                    }
                }
                withContext(Dispatchers.Main) {
                    val insertCatResponse = lifecycleScope.launch {
                        Log.d(
                            TAG,
                            "cacheCatListAndInsertInLocalDB: Before Inserting In Local Db --> ${catList.toString()}"
                        )
                        if (catList.isNotEmpty()) {
                            viewModel.insertCatItem(catList)
                        }
                    }
                    insertCatResponse.join()
                    val requiredCatFolders: ArrayList<CategoriesModel> =
                        viewModel.getCategoriesDataIfServIdNull() as ArrayList<CategoriesModel>
                    createFoldersOnServer(requiredCatFolders)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "cacheCatListAndInsertInLocalDB: ${e.message}")
            logout("cacheCatListAndInsertInLocalDB")
        }
    }

    suspend fun createFoldersOnServer(requiredCatFolders: ArrayList<CategoriesModel>) {
        Log.d(TAG, "createFoldersOnServer: Called")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (requiredCatFolders.isNotEmpty()) {
                    Log.d(
                        TAG,
                        "createFoldersOnServer: Require Folders to be create in Server ${requiredCatFolders.toString()}"
                    )
                    requiredCatFolders.forEach { catModel ->
                        val folderMime = "application/vnd.google-apps.folder"
                        if (catModel.catType == folderMime) {
                            val fileMetadata = com.google.api.services.drive.model.File()
                            fileMetadata.name = catModel.catId
                            fileMetadata.parents = Collections.singletonList("appDataFolder")
                            fileMetadata.mimeType = folderMime

                            val file = getDriveService()?.let {
                                it.files().create(fileMetadata)
                                    .setFields("id,name,parents")
                                    .execute()
                            }
                            Log.d(TAG, "Created New Folder : ${file?.id} ${file?.name}")
                            file?.let {
                                val res =
                                    viewModel.updateCatServId(it.name, it.id, it.parents.get(0))
                                Log.d(
                                    TAG,
                                    "createFoldersOnServer: UpdateCatServId in Local DB ---> $res"
                                )
                            }
                        }
                    }
//                    val catListData=viewModel.getCategoriesData()
//                    Log.d(TAG, "createFoldersOnServer: ${catListData.toString()}")
                }
                getSubFoldersDataCache()
            } catch (e: Exception) {
                e.printStackTrace()
                FileSize.showSnackBar(e.message.toString(), binding.root)
                Log.d(TAG, "createFoldersOnServer: Exception Occured ->${e.message}")
                logout("createFoldersOnServer()")
            }
        }
    }

    suspend fun getSubFoldersDataCache() {
        Log.d(TAG, "getSubFoldersDataCache: Called")
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                val catListData = viewModel.getCategoriesData()
                val foldersList = ArrayList<FolderTable>()
                val mimetype = this@MainActivity.resources.getString(R.string.folder_mime_type)
                if (catListData.isNotEmpty()) {
                    catListData.forEach { catModel ->
                        Log.d(
                            TAG,
                            "getSubFoldersDataCache: SubFolder to be Cache in Local ${catListData.toString()}"
                        )
                        val result = getDataBasedOnParentId(catModel.serverId, mimetype)
                        result?.let { filesList ->
                            if (filesList.files.isNotEmpty()) {
                                filesList.files.forEach { file ->
                                    val driveTimeStamp =
                                        DateUtills.driveDateToTimeStamp(file.createdTime.toString())
                                    val folderTable = FolderTable(
                                        0,
                                        file.name,
                                        driveTimeStamp.toString(),
                                        catModel.catId,
                                        file.id,
                                        false
                                    )
                                    foldersList.add(folderTable)
                                }
                                if (foldersList.isNotEmpty()) viewModel.insertFoldertList(
                                    foldersList
                                )
                            }
                        }
                    }
                    getItemDataCache()
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "getSubFoldersDataCache: ${e.message}")
            logout("getSubFoldersDataCache()")
        }
    }

    suspend fun getItemDataCache() {
        Log.d(TAG, "getItemDataCache: Called")
        try {
            val foldDataList = viewModel.getFoldersDataList()
            val itemModelList = ArrayList<ItemModel>()
            val mimetype = "*/*"
            val res = lifecycleScope.launch(Dispatchers.IO) {
                if (foldDataList.isNotEmpty()) {
                    foldDataList.forEach { folTable ->
                        Log.d(
                            TAG,
                            "getItemDataCache: Item Data Cache in Local Db ${foldDataList.toString()}"
                        )
                        val result: FileList? = gDriveService.let {
                            it.files().list()
                                .setSpaces("appDataFolder")
                                .setQ("'${folTable.folderServerId}' in parents")
                                .setFields("nextPageToken, files(permissions,id,name,size,parents,mimeType,quotaBytesUsed,modifiedTime,createdTime)")
                                .execute()
                        }
                        result?.let {
                            Log.d(TAG, "getItemDataCache Size: ${it.files.size}")
                            if (it.files.isNotEmpty()) {
                                it.files.forEach { file ->
                                    val driveTimeStamp =
                                        DateUtills.driveDateToTimeStamp(file.createdTime.toString())
                                    val defLocation =
                                        this@MainActivity.resources.getString(R.string.db_folder_path)
                                    val catNameCreation = "app_" + folTable.folderCatType
                                    val path = defLocation + catNameCreation
                                    val itemModel = ItemModel(
                                        0,
                                        file.name,
                                        file.quotaBytesUsed.toString(),
                                        driveTimeStamp.toString(),
                                        file.mimeType,
                                        path,
                                        file.id,
                                        folTable.folderId.toString(),
                                        folTable.folderCatType,
                                        false
                                    )
                                    itemModelList.add(itemModel)
                                    Log.d(TAG, "getItemDataCache: ${itemModel.toString()}")
                                }
                            }
                        }
                    }
                    Log.d(TAG, "getItemDataCache: ${itemModelList.toString()}")
                    if (itemModelList.isNotEmpty()) {
                        Log.d(TAG, "getItemDataCache: ${itemModelList.toString()}")
                        viewModel.insertItemsList(itemModelList)
                    }
                }
            }
            res.join()
            openIntent()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "getItemDataCache: ${e.message}")
            logout("getItemDataCache()")
        }
    }

    suspend fun getDataBasedOnParentId(parentFolderId: String, mimeType: String): FileList? {
        Log.d(TAG, "getDataBasedOnParentId: Called")
        val pagetoken: String? = null
        var result: FileList? = null
        try {
            do {
                result = gDriveService.files().list().apply {
                    spaces = "appDataFolder"
                    q = "'${parentFolderId}' in parents and mimeType='${mimeType}'"
                    fields =
                        "nextPageToken, files(id,name,size,parents,mimeType,quotaBytesUsed,modifiedTime,createdTime)"
                    pageToken = this.pageToken
                }.execute()

            } while (pagetoken != null)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "getDataBasedOnParentId: ${e.message}")
            logout("getDataBasedOnParentId")
        }
        return result
    }

    fun deleteData(fileId: String) {
        try {
            val files: Void? = getDriveService()?.let {
                it.files().delete(fileId).execute()
            }
            Log.d(TAG, "deleteData: $files")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "deleteData: ${e.message}")
        }
    }

}