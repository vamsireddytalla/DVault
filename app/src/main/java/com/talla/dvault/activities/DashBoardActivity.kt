package com.talla.dvault.activities

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.talla.dvault.R
import com.talla.dvault.databinding.ActivityDashBoardBinding
import android.app.Activity
import android.graphics.Color


private const val TAG = "DashBoardActivity"

class DashBoardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashBoardBinding
    var isNightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val appSettingsPrefs = getSharedPreferences("settings", 0)
        val prefEdit = appSettingsPrefs.edit()
        isNightMode = appSettingsPrefs.getBoolean("NightMode", false)


        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            checkDarkMode()
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            checkDarkMode()
        }


        binding.themeBtn.setOnClickListener {
            if (isNightMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                prefEdit.putBoolean("NightMode", false)
                prefEdit.apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                prefEdit.putBoolean("NightMode", true)
                prefEdit.apply()
            }

        }

    }

    fun checkDarkMode() {
        if (isNightMode) {
            binding.themeBtn.setImageResource(R.drawable.moon)
            Log.d(TAG, "onCreate: Light")
        } else {
            binding.themeBtn.setImageResource(R.drawable.sun)
            Log.d(TAG, "onCreate: Night")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val brightTODark = binding.themeBtn.drawable as AnimatedVectorDrawable
            brightTODark.start()
        } else {
            Log.d(TAG, "checkDarkMode: Below Lollipop Version")
        }


    }



}