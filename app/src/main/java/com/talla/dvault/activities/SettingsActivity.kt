package com.talla.dvault.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.talla.dvault.R
import com.talla.dvault.databinding.ActivitySettingsBinding
import com.talla.dvault.databinding.SettingsBtmSheetBinding
import com.talla.dvault.services.DriveService
import com.talla.dvault.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import android.widget.CompoundButton
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.database.entities.User
import com.talla.dvault.databinding.StorageLayoutBinding
import com.talla.dvault.preferences.UserPreferences
import com.talla.dvault.preferences.UserPreferences.Companion.dataStore
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.utills.FileSize
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import javax.inject.Inject


private const val TAG = "SettingsActivity"

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private var isLockEnabled: Boolean = false
    var bol = false
    private var driveBgService: DriveService? = null
    private var binder: DriveService.LocalBinder? = null
    private val viewModel: SettingsViewModel by viewModels()
    @Inject
    lateinit var userPreferences:UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            finish()
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.getAppLockStatus().observe(this@SettingsActivity) {
                it?.let { appLockModel ->
                    isLockEnabled = appLockModel.isLocked
                    binding.appLock.isChecked = appLockModel.isLocked
                    Log.d(TAG, "LockModel Absorption ${appLockModel.toString()}")
                }
            }
        }


        binding.appLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isLockEnabled) {
                    binding.appLock.isChecked = false
                    var intent = Intent(this@SettingsActivity, SecureQueActivity::class.java)
                    startActivity(intent)
                }
            } else {
                runBlocking {
                    isLockEnabled = false
                    var res = viewModel.disableAppLock()
                    Log.d(TAG, "onCreate: ${res}")
                }
            }
        }


        lifecycleScope.async {
            viewModel.checkDataAndGetCount().observe(this@SettingsActivity, Observer {
                Log.d(TAG, "Observed Data: $it")
            })
        }


        binding.apply {
            backupBtn.setOnClickListener {
                showBottomSheetDialog(this@SettingsActivity.resources.getString(R.string.backup))
            }
            restoreBtn.setOnClickListener {
                showBottomSheetDialog(this@SettingsActivity.resources.getString(R.string.restore))
            }
            backUpCancel.setOnClickListener {
                FileSize.backUpRestoreEnabled = false
                backupRoot.visibility = View.VISIBLE
                backUpProgressRoot.visibility = View.GONE
                binder?.stopSettingsService("Back-up Cancelled!")
            }
            cancelRestore.setOnClickListener {
                FileSize.backUpRestoreEnabled = false
                binding.restoreRoot.visibility = View.VISIBLE
                binding.restoreProgressRoot.visibility = View.GONE
                binder?.stopSettingsService("Restore-Cancelled!")
            }
        }

       lifecycleScope.launch {
           userPreferences.getData(UserPreferences.LAST_BACKUP_TIME).collect { value ->
               withContext(Dispatchers.Main){
                   if (value.isNotEmpty())
                   {
                       binding.lastBackUp.text="Last Backup at\n"+value
                   }

               }
           }
       }

    }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            binder = service as DriveService.LocalBinder
            driveBgService = binder?.getService()
            binder?.settingsBRCallback(object :DriveService.SettingsCalBack{
                override fun fileServerDealing(progress: Int, mbCount: String, totalItems: String) {
                    Log.d(TAG, "File New Dealing : $progress $mbCount $totalItems")
                    lifecycleScope.launch(Dispatchers.IO) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (FileSize.settingsBRSelected == this@SettingsActivity.resources.getString(R.string.backup)) {
                                binding.backupProgress.progress = progress
                                binding.backUpMbTransfer.text = mbCount
                                binding.totalCount.text = totalItems
                                binding.backupProgress.isIndeterminate=false
                                if (!FileSize.backUpRestoreEnabled || mbCount == "Done") {
                                    binding.backupRoot.visibility = View.VISIBLE
                                    binding.backUpProgressRoot.visibility = View.GONE
                                    showSnackBar(mbCount)
                                } else {
                                    binding.backupRoot.visibility = View.GONE
                                    binding.backUpProgressRoot.visibility = View.VISIBLE
                                }
                            } else {
                                binding.restoreProgress.progress = progress
                                binding.totalMbRestore.text = mbCount
                                binding.restorePercent.text = totalItems
                                binding.restoreProgress.isIndeterminate=false
                                if (!FileSize.backUpRestoreEnabled || mbCount == "") {
                                    binding.restoreRoot.visibility = View.VISIBLE
                                    binding.restoreProgressRoot.visibility = View.GONE
                                    showSnackBar(mbCount)
                                } else {
                                    binding.restoreRoot.visibility = View.GONE
                                    binding.restoreProgressRoot.visibility = View.VISIBLE
                                }
                            }


                        }
                    }
                }

                override fun storageQuote(usedStorage: String,totalStorage: String) {
                    Log.d(TAG, "storageQuote: $totalStorage $usedStorage")
                    var usedOutPut=totalStorage?.let {
                        it.length-3
                    }
                    var totalOutPut=usedStorage?.let {
                        it.length-3
                    }
                    var storageBinding=StorageLayoutBinding.bind(binding.root)
                    lifecycleScope.launch(Dispatchers.Main) {
                        storageBinding.storageProgress.max=totalOutPut
                        storageBinding.storageProgress.progress=usedOutPut
                        storageBinding.totalSpace.text=totalStorage
                        storageBinding.usedSpace.text=usedStorage
                    }
                }

            })
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            binder = null
            Log.d(TAG, "onServiceDisconnected: Called")
        }
    }

    override fun onStart() {
        super.onStart()
        if (binder == null) {
            Intent(this, DriveService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun showBottomSheetDialog(title: String) {
        var bsd = BottomSheetDialog(this, R.style.bottomSheetStyle)
        var bottomView: View? = null
        var sheetBinding: SettingsBtmSheetBinding? = null
        sheetBinding = SettingsBtmSheetBinding.inflate(layoutInflater)
        bottomView = sheetBinding.root
        bsd.setContentView(bottomView)
        bsd.setCanceledOnTouchOutside(true)
        FileSize.selectedBackRestore.clear()
        sheetBinding.apply {
            titleSheet.text = title
            bkpBtn.text = title
            img.setColorFilter(ContextCompat.getColor(this@SettingsActivity, R.color.light_pink))
            audioImg.setColorFilter(
                ContextCompat.getColor(
                    this@SettingsActivity,
                    R.color.light_yellow
                )
            )
            docImg.setColorFilter(ContextCompat.getColor(this@SettingsActivity, R.color.light_blue))
            vdoImg.setColorFilter(
                ContextCompat.getColor(
                    this@SettingsActivity,
                    R.color.light_violet
                )
            )

            checkAll.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                imageCheck.isChecked = isChecked
                audioCheckBox.isChecked = isChecked
                docsCheckBox.isChecked = isChecked
                vdoCheckBox.isChecked = isChecked
                if (!isChecked) FileSize.selectedBackRestore.clear()
            })
            imageCheck.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) FileSize.selectedBackRestore.add("Img") else FileSize.selectedBackRestore.remove(
                    "Img"
                )
                checkAll.isChecked = FileSize.selectedBackRestore.size == 4
            })
            audioCheckBox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) FileSize.selectedBackRestore.add("Aud") else FileSize.selectedBackRestore.remove(
                    "Aud"
                )
                checkAll.isChecked = FileSize.selectedBackRestore.size == 4
            })
            docsCheckBox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) FileSize.selectedBackRestore.add("Doc") else FileSize.selectedBackRestore.remove(
                    "Doc"
                )
                checkAll.isChecked = FileSize.selectedBackRestore.size == 4
            })
            vdoCheckBox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) FileSize.selectedBackRestore.add("Vdo") else FileSize.selectedBackRestore.remove(
                    "Vdo"
                )
                checkAll.isChecked = FileSize.selectedBackRestore.size == 4
            })

            bkpBtn.setOnClickListener {
                if (FileSize.selectedBackRestore.isNotEmpty()
                    && !FileSize.UNLOCK_FILE_COPYING
                    && !FileSize.FILE_COPYING
                    && !FileSize.backUpRestoreEnabled
                    && (binding.restoreProgress.isVisible || binding.backupProgress.isVisible)
                ) {
                    var backupstring = bkpBtn.text.toString()
                    if (title == this@SettingsActivity.resources.getString(R.string.backup)) {
                        binding.backUpMbTransfer.text=getString(R.string.initializing)
                        FileSize.settingsBRSelected = backupstring
                        FileSize.backUpRestoreEnabled = true
                        binding.backupRoot.visibility = View.GONE
                        binding.backUpProgressRoot.visibility = View.VISIBLE
                    } else {
                        binding.totalMbRestore.text=getString(R.string.initializing)
                        var restoreString = this@SettingsActivity.resources.getString(R.string.restore)
                        FileSize.settingsBRSelected = restoreString
                        FileSize.backUpRestoreEnabled = true
                        binding.restoreRoot.visibility = View.GONE
                        binding.restoreProgressRoot.visibility = View.VISIBLE
                    }
                  lifecycleScope.launch(Dispatchers.IO) {
                      userPreferences.storeStringData(UserPreferences.LAST_BACKUP_TIME,
                          DateUtills.getLastBackUpTime(this@SettingsActivity)!!)
                  }
                    binder?.startBackUpService(title)
                } else {
                    if (FileSize.UNLOCK_FILE_COPYING) {
                        showSnackBar("File Unlocking is In Progress Please wait...")
                    } else if (FileSize.FILE_COPYING) {
                        showSnackBar("File Copying is In Progress Please wait...")
                    } else if (FileSize.backUpRestoreEnabled) {
                        showSnackBar("Processing another task Please wait")
                    }
                }

                bsd.dismiss()

            }

        }


        bsd.show()
    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }


}