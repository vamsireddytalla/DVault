package com.talla.dvault.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.talla.dvault.databinding.ActivityAppLockBinding

class AppLockActivity : AppCompatActivity()
{
    private lateinit var binding:ActivityAppLockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}