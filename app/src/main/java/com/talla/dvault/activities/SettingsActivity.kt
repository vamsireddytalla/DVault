package com.talla.dvault.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.databinding.ActivitySettingsBinding
import com.talla.dvault.viewmodels.AppLockViewModel
import com.talla.dvault.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

private const val TAG = "SettingsActivity"

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private var isLockEnabled: Boolean = false
    var bol = false
    private val viewModel: SettingsViewModel by viewModels()

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
                    isLockEnabled=false
                    var res = viewModel.disableAppLock()
                    Log.d(TAG, "onCreate: ${res}")
                }
            }
        }

    }

    fun backUp(view: android.view.View)
    {
        lifecycleScope.launch(Dispatchers.IO)
        {
            val rs=viewModel.lockChange(bol)
            Log.d(TAG, "backUp: ${rs}")
            bol = !bol
        }
    }


}