package com.talla.dvault.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.talla.dvault.databinding.ActivityFoldersBinding

class FoldersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFoldersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    fun backBtn(view: android.view.View) {
        finish()
    }

}