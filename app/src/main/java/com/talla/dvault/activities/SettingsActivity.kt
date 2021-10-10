package com.talla.dvault.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.talla.dvault.databinding.ActivitySettingsBinding

private const val TAG = "SettingsActivity"
class SettingsActivity : AppCompatActivity()
{
    private lateinit var binding:ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            finish()
        }


        binding.appLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
            Log.d(TAG, "Checked")
            else
            Log.d(TAG, "Un Checked")
        }

    }
}