package com.talla.dvault.activities

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.talla.dvault.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject


private const val TAG = "DashBoardActivity"

@AndroidEntryPoint
class DashBoardActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityDashBoardBinding
    @Inject
    lateinit var appSettingsPrefs:UserPreferences
    @Inject
    lateinit var glide: RequestManager
    @Inject
    lateinit var gso:GoogleSignInOptions
    private lateinit var dialog:Dialog
    private lateinit var user:User
    private lateinit var customDialogProfileBinding:CustomDialogProfileBinding
    private lateinit var requestPermissionLauncher:ActivityResultLauncher<String>
    private val viewModel:MainViewModel by viewModels()
    var isNightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch(Dispatchers.Default) {

            withContext(Dispatchers.Main){
                user=viewModel.getUserObj()
                binding.userName.text=user.userName
                glide.load(user.userImage).into(binding.userProfilePic)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    binding.userProfilePic.clipToOutline=true
                }
            }
        }

        viewModel.getLiveData().observe(this, Observer{
            it?.let {
                it.forEach { catModel->
                    if (catModel.catId.equals("Img")) binding.totalImages.setText(catModel.totalItems.toString())
                    if (catModel.catId.equals("Vdo")) binding.totalVIdeos.setText(catModel.totalItems.toString())
                    if (catModel.catId.equals("Doc")) binding.totalDocs.setText(catModel.totalItems.toString())
                    if (catModel.catId.equals("Aud")) binding.totalAudios.setText(catModel.totalItems.toString())
                }
            }
        })

        lifecycleScope.launch(Dispatchers.IO) {
            appSettingsPrefs.getBooleanData(UserPreferences.NIGHT_MODE).collect { value ->
                withContext(Dispatchers.Main){
                    if (value is Boolean)
                    {
                        isNightMode=value
                    }else{
                        isNightMode=false
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

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Permission: Granted")
            } else {
                var result=ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                Log.d(TAG, "Permission: Denied ${result}")
            }
        }

        binding.settingsBtn.setOnClickListener {
            val intent: Intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.audioSelection.setOnClickListener {
            val intent: Intent = Intent(this, FoldersActivity::class.java)
            intent.putExtra(getString(R.string.cat_key),"Aud")
            startActivity(intent)
        }
        binding.docSelection.setOnClickListener {
            val intent: Intent = Intent(this, FoldersActivity::class.java)
            intent.putExtra(getString(R.string.cat_key),"Doc")
            startActivity(intent)
        }
        binding.videoSelection.setOnClickListener {
            val intent: Intent = Intent(this, FoldersActivity::class.java)
            intent.putExtra(getString(R.string.cat_key),"Vdo")
            startActivity(intent)
        }
        binding.imageSelection.setOnClickListener {
            val intent: Intent = Intent(this, FoldersActivity::class.java)
            intent.putExtra(getString(R.string.cat_key),"Img")
            startActivity(intent)
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

    private fun showLocationMandatoryDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Turn On Gps")
        alertDialogBuilder.setMessage("Please allow permission to get nearest shops to you.")
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton("Ok") { dialogInterface, i ->
            dialogInterface.dismiss()
        }.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
            finish()
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
            customDialogProfileBinding.userProfilePic.clipToOutline=true
        }
        customDialogProfileBinding.userName.text=user.userName
        customDialogProfileBinding.userEmail.text=user.userEmail
        customDialogProfileBinding.lastLoggedin.text=user.userloginTime
        glide.load(user.userImage).into(customDialogProfileBinding.userProfilePic)
        customDialogProfileBinding.login.setOnClickListener(View.OnClickListener {
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            mGoogleSignInClient?.signOut()
                ?.addOnCompleteListener(this, object : OnCompleteListener<Void> {
                    override fun onComplete(task: Task<Void>) {
                        Toast.makeText(this@DashBoardActivity, "Signed Out", Toast.LENGTH_SHORT).show()
                        openIntent()
                    }
                })
            dialog.dismiss() })
    }

    fun profileRoot(view: android.view.View)
    {
        showPofileDialog()
    }

    private fun openIntent() {
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


    fun View.showSnackbar(view: View, msg: String, length: Int, actionMessage: CharSequence?, action: (View) -> Unit)
    {
        val snackbar = Snackbar.make(view, msg, length)
        if (actionMessage != null) {
            snackbar.setAction(actionMessage) {
                action(this)
            }.show()
        } else {
            snackbar.show()
        }
    }






}