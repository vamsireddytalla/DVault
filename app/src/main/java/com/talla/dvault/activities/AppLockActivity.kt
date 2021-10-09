package com.talla.dvault.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.databinding.ActivityAppLockBinding

class AppLockActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppLockBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    fun nextBtn(view: android.view.View) {
        val password = binding.password.text.toString().trim()
        val cnf_password = binding.cnfPassword.text.toString().trim()

        if (password.isBlank() || cnf_password.isBlank()) {
            showSnackBar("Enter Both Fields")
            return
        }else if (!password.equals(cnf_password))
        {
            showSnackBar("Both Passwords are Not Same")
            return
        }else{

        }

    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }


}