package com.talla.dvault

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.talla.dvault.activities.DashBoardActivity
import com.talla.dvault.databinding.ActivityMainBinding
import dagger.Provides
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class MainActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var gso:GoogleSignInOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Handler().postDelayed({
            binding.motion1.transitionToEnd()
        }, 1000)
    }


    private fun openIntent() {
        val intent: Intent = Intent(this, DashBoardActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun googleSIgnIn(view: android.view.View)
    {
        openIntent()
    }


    fun isUserSignedIn(): Boolean {
        val googleSigninAccount = GoogleSignIn.getLastSignedInAccount(this)
        return googleSigninAccount != null
    }

}