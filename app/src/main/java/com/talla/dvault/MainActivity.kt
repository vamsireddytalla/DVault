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
import com.talla.dvault.database.entities.ItemModel
import com.talla.dvault.database.entities.User
import com.talla.dvault.databinding.ActivityMainBinding
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

    }

    private fun openIntent() {
        lifecycleScope.launch {
            val res = viewModel.isLoggedInPerfectly()
            if (res >= 8 && isUserSignedIn()) {
                Log.d(TAG, "openIntent: Called")
                val intent: Intent = Intent(this@MainActivity, DashBoardActivity::class.java)
                startActivity(intent)
                finish()
                exitProcess(0)
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
                                showProgressDialog()
                                var driveScope = lifecycleScope.launch(Dispatchers.Default) {
                                    getDriveFiles(user)
                                }
                                driveScope.join()
                                stopProgressDialog()
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

    suspend fun getDriveFiles(user: User) {
        try {
            getDriveService()?.let { gdService ->
                var pagetoken: String? = null
                do {
                    val result: FileList = gdService.files().list().apply {
                        spaces = "appDataFolder"
                        fields = "nextPageToken, files(id,name,mimeType,quotaBytesUsed)"
                        pageToken = this.pageToken
                    }.execute()

                    result?.let { res ->
                        Log.d(TAG, "getDriveFiles Count From Server : ${res.files}")
                        Log.d(TAG, "getDriveFiles: Server Files Count  ${res.files.size}")
                        if (res.files.size < 8) {
                            Log.d(TAG, "getDriveFiles: New User")
                            withContext(Dispatchers.Main) {
                                var res = viewModel.insertData(user)
                            }
                            //Upload local Databases to Server and get ids and store in local db and update in server again
//                            res.files.forEach {
//                                deleteData(it.id)
//                            }
                            createFolder(res)
                            FileSize.showSnackBar("Welcome !", binding.root)
                        } else {
                            //Download Database files from server .Delete local db files and Replace new files in that
                            Log.d(TAG, "getDriveFiles: Old User")
                            var catList = ArrayList<CategoriesModel>()
                            res.files.forEach { fileee ->
                                Log.d(
                                    TAG,
                                    "getDriveFiles: Server File Sizes ${FileSize.bytesToHuman(fileee.quotaBytesUsed)} ${fileee.name}"
                                )
                                var name = ""
                                when (fileee.name) {
                                    "Img" -> name = "Images"
                                    "Aud" -> name = "Audios"
                                    "Vdo" -> name = "Videos"
                                    "Doc" -> name = "Docs"
                                    "DB" -> name = "DataBases"
                                    "DVault.db" -> name = "DVault.db"
                                    "DVault.db-wal" -> name = "DVault.db-wal"
                                    "DVault.db-shm" -> name = "DVault.db-shm"
                                }
                                var catModel =
                                    CategoriesModel(fileee.name, name, fileee.id, fileee.mimeType)
                                catList.add(catModel)
                            }
                            Log.d(TAG, "Category List Retrieved: ${catList.toString()}")
                            FileSize.showSnackBar("Welcome Back!", binding.root)
                            withContext(Dispatchers.Main) {
                                Log.d(TAG, "getDriveFiles: ${Thread.currentThread().name}")
                                Log.d(TAG, "getDriveFiles: ${catList.size}")
                                catList.forEach {
                                    var res = viewModel.updateCatItem(it)
                                    Log.d(TAG, "getDriveFiles: Updated Res-> $res")
                                }
                                val userRes = viewModel.insertData(user)
                                Log.d(TAG, "getDriveFiles: After User Data Insertion $userRes")
                                if (userRes > 0) {
                                    downloadDbFiles()
                                } else {
                                    logout("getDriveFiles()")
                                    showSnackBar("Error occured retry")
                                }
                            }
                        }
                    }

                } while (pagetoken != null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FileSize.showSnackBar(e.message.toString(), binding.root)
            Log.d(TAG, "getDriveFiles: Exception Occured -> ${e.message}")
            logout("getDriveFiles()")
        }
    }

    suspend fun createFolder(res: FileList) {
        try {
            viewModel.resetCatServerId()
            var deleteJob = lifecycleScope.async(Dispatchers.IO) {
                res.files.forEach { serverFile ->
                    deleteData(serverFile.id)
                }
            }
            deleteJob.await()
            val categoriesListNew: ArrayList<CategoriesModel> =
                viewModel.getCategoriesData() as ArrayList<CategoriesModel>
            categoriesListNew.forEach { catModel ->
                var folderMime = "application/vnd.google-apps.folder"
                if (catModel.catType == folderMime) {
                    val fileMetadata = com.google.api.services.drive.model.File()
                    fileMetadata.name = catModel.catId
                    fileMetadata.parents = Collections.singletonList("appDataFolder")
                    fileMetadata.mimeType = folderMime

                    val file = getDriveService()?.let {
                        it.files().create(fileMetadata)
                            .setFields("id,name")
                            .execute()
                    }
                    Log.d(TAG, "Created New Folder : ${file?.id} ${file?.name}")
                    file?.let {
                        var res = viewModel.updateCatServId(it.name, it.id)
                        Log.d(TAG, "createFolder: UpdateCatServId $res")
                    }
                }
            }
            val catModel = viewModel.getDbServerFolderId("DB")
            val categoriesListFinal: ArrayList<CategoriesModel> =
                viewModel.getCategoriesData() as ArrayList<CategoriesModel>
            Log.d(TAG, "Local Db serverFolder Id---> ${catModel.serverId}")
            Log.d(TAG, "createFolder: ${categoriesListFinal.toString()}")
            if (catModel.serverId.isNotEmpty()) {
                Log.d(TAG, "DB Folder : server Id is avilable")
                uploadData(catModel.serverId)
            } else {
                stopProgressDialog()
                FileSize.showSnackBar("DB folder not created retry.", binding.root)
                Log.d(TAG, "createFolder: No Server Id Found in Database")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            FileSize.showSnackBar(e.message.toString(), binding.root)
            Log.d(TAG, "createFolder: Exception Occured ->${e.message}")
            logout("createFolder()")
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

    suspend fun uploadData(folderServId: String) {
        try {
            var fileRootPath = this.resources.getString(R.string.db_path)
            val fileMetadata = com.google.api.services.drive.model.File()
            val children: Array<String> = File(fileRootPath).list()
            var uploadJob = lifecycleScope.launch(Dispatchers.IO) {
                for (i in children.indices) {
                    var dbFilePath = File(fileRootPath, children[i])
                    fileMetadata.name = dbFilePath.name
                    fileMetadata.parents = Collections.singletonList(folderServId)
                    val mediaContent = FileContent("file/*", dbFilePath)
                    val fileRes = getDriveService()?.let {
                        it.files().create(fileMetadata, mediaContent)
                            .setFields("id, parents,name,quotaBytesUsed")
                            .execute()
                    }

                    fileRes?.let {
                        Log.d(
                            TAG,
                            "Database Files Uploaded : ${fileRes.id} ${fileRes.name} ${fileRes.quotaBytesUsed}"
                        )
                        viewModel.updateCatServId(it.name.toString(), it.id.toString())
                    }
                }
            }
            uploadJob.join()
            val catDbLocalFileList = viewModel.getCategoriesIfNotEmpty()
            Log.d(TAG, "uploadData: ${catDbLocalFileList.toString()}")
            Log.d(TAG, "uploadData: CatList Check Server Ids List ${catDbLocalFileList.size}")
            var updateJob = lifecycleScope.launch(Dispatchers.IO) {
                catDbLocalFileList.forEach {
                    updateDbFiles(getDriveService()!!, it.serverId, it.catId)
                }
            }
            updateJob.join()
            openIntent()
        } catch (e: Exception) {
            e.printStackTrace()
            FileSize.showSnackBar(e.message.toString(), binding.root)
            Log.d(TAG, "uploadData: Exception Occured->${e.message}")
            logout("uploadData()")
        }
    }

    //quotaBytesUsed
    private fun updateDbFiles(service: Drive, fileId: String, fileName: String) {
        try {

            var mimeType = "file/*"
            // File's new content.
            val file = File(this.resources.getString(R.string.db_path) + fileName)
            val newMetadata = com.google.api.services.drive.model.File()
            newMetadata.name = file.name

            // Convert content to an AbstractInputStreamContent instance.
            val contentStream = ByteArrayContent.fromString(mimeType, file.toString())
            val mediaContentNew =
                InputStreamContent("file/*", BufferedInputStream(FileInputStream(file)))
            // Send the request to the API.
            var fileRes = service.files().update(fileId, newMetadata, mediaContentNew)
                .setFields("id, name, appProperties,quotaBytesUsed").execute()
            fileRes?.let {
                Log.d(
                    TAG,
                    "UpdateDbFiles: Successfully ${fileRes.name} ${fileRes.id} ${fileRes.quotaBytesUsed}"
                )
            }
        } catch (e: IOException) {
            logout("updateDbFiles()")
            println("An error occurred: $e")
            Log.d(TAG, "UpdateDbFile Error --> : ${e.message}")

        }
    }

    suspend fun getFilesUnderDBFolder(
        DbFolderId: String,
        categoriesList: ArrayList<CategoriesModel>
    ): List<CategoriesModel> {
        var dbList = categoriesList
        Log.d(TAG, "getFilesUnderDBFolder: $DbFolderId")
        val files: FileList? = getDriveService()?.let {
            it.files().list()
                .setSpaces("appDataFolder")
                .setQ("'${DbFolderId}' in parents")
                .setFields("nextPageToken, files(id, name)")
                .execute()
        }
        files?.let {
            Log.d(TAG, "Db Files Under Folder in Server : Total List  ${it.files.size}")
            for (file in it.files) {
                for ((index, source) in dbList.withIndex()) {
                    if (source.catType == "files/*") {
                        if (file.name.contentEquals(source.catId)) {
                            Log.d(
                                TAG,
                                "getFilesUnderDBFolder: Files Under Db Folder Existed ${file.name.toString()}"
                            )
                            viewModel.updateCatServId(file.name.toString(), file.id.toString())
                        }
                    }
                }
            }
        }
        val catListData: ArrayList<CategoriesModel> =
            viewModel.getCategoriesData() as ArrayList<CategoriesModel>
        return catListData
    }

    suspend fun downloadDbFiles() {
        try {
            val categoriesList: ArrayList<CategoriesModel> =
                viewModel.getCategoriesIfNotEmpty() as ArrayList<CategoriesModel>
            Log.d(TAG, "downloadDbFiles: Before Download LogFile ${categoriesList.toString()}")

            var downloadFileJob = lifecycleScope.launch(Dispatchers.IO) {
                var file = java.io.File(this@MainActivity.resources.getString(R.string.db_path))
                deleteAllDbFiles(file)
                categoriesList.forEach {
                    downloadFile(it.serverId, it.catId)
                }
            }
            downloadFileJob.join()
            val res = viewModel.isLoggedInPerfectly()
            var newFile = java.io.File(this@MainActivity.resources.getString(R.string.db_path))
            if (newFile.list().size >= 3 && isUserSignedIn() && res >= 8) {
                FileSize.showSnackBar("Welcome Back!", binding.root)
                Log.d(TAG, "downloadDbFiles: Ready to open Intent")
                openIntent()
            } else {
                FileSize.showSnackBar("Error occured retry", binding.root)
                logout("downloadDbFiles()")
            }
        } catch (e: Exception) {
            logout("downloadDbFiles()")
            e.printStackTrace()
            Log.d(TAG, "downloadDbFiles: ${e.message}")
        }
    }

    fun downloadFile(fileId: String, fileName: String) {
        try {
            Log.d(TAG, "downloadFile: ${fileId}  ${fileName}")
            var file = java.io.File(this@MainActivity.resources.getString(R.string.db_path))

            val out: OutputStream = FileOutputStream("$file/$fileName")
            val request: Drive.Files.Get? =
                getDriveService()?.files()?.get(fileId)?.setFields("id, name, appProperties")
            request?.let {
                it.executeMediaAndDownloadTo(out)
                Log.d(TAG, "downloadedFile: ${it.fileId}")
            }
//                out.flush()
//                out.close()
//                request?.clear()
        } catch (e: Exception) {
            logout("downloadFile()")
            e.printStackTrace()
            Log.d(TAG, "downloadFile: ${e.message}")
        }
    }

    fun logout(message: String) {
        Log.d(TAG, "logout Called From $message")
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mGoogleSignInClient?.signOut()
            ?.addOnCompleteListener(
                this
            ) { }
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

    fun deleteAllDbFiles(file: File) {
        if (file.isDirectory) {
            val children: Array<String> = file.list()
            for (i in children.indices) {
                File(file, children[i]).delete()
            }
        }
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

}