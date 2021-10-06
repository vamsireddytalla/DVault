package com.talla.dvault

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.talla.dvault.activities.DashBoardActivity
import com.talla.dvault.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Handler().postDelayed({
            binding.motion1.transitionToEnd()
        }, 1000)
    }


    private fun isUserSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        return account != null
    }

    private fun openIntent() {
        val intent: Intent = Intent(this, DashBoardActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun googleSIgnIn(view: android.view.View)
    {
//        if (!isUserSignedIn())
//        {
//
//        }
        openIntent()
    }

}