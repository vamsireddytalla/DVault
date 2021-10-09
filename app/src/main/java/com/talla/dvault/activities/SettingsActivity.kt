package com.talla.dvault.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.talla.dvault.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity()
{
    private lateinit var binding:ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.setPin.setOnClickListener{
            val intent=Intent(this,SecureQueActivity::class.java)
            startActivity(intent)
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

    }
}