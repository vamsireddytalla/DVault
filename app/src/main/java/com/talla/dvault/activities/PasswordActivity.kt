package com.talla.dvault.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.databinding.ActivityPasswordBinding
import com.talla.dvault.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

private const val TAG = "PasswordActivity"
@AndroidEntryPoint
class PasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val view = currentFocus
        if (view != null && (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_MOVE) && view is EditText && !view.javaClass.name.startsWith(
                "android.webkit."
            )
        ) {
            val scrcoords = IntArray(2)
            view.getLocationOnScreen(scrcoords)
            val x = ev.rawX + view.getLeft() - scrcoords[0]
            val y = ev.rawY + view.getTop() - scrcoords[1]
            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom()) (this.getSystemService(
                INPUT_METHOD_SERVICE
            ) as InputMethodManager).hideSoftInputFromWindow(
                this.window.decorView.applicationWindowToken, 0
            )
        }
        return super.dispatchTouchEvent(ev)
    }

    fun checkPassword(view: android.view.View) {
        var enteredPass=binding.password.text.toString().trim()
        if (enteredPass.isBlank())
        {
            showSnackBar("Enter Password")
            binding.password.requestFocus()
            return
        }else if (enteredPass.length<4)
        {
            showSnackBar("Check Entered Password")
            binding.password.requestFocus()
            return
        }else{
           runBlocking {
               var res=viewModel.checkEnteredPassword(enteredPass)
               if (res>0)
               {
                   var intent=Intent(this@PasswordActivity,DashBoardActivity::class.java)
                   startActivity(intent)
               }else{
                   showSnackBar("wrong")
                   Log.d(TAG, "checkPassword: Wrong pass")
               }
           }
        }
    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    fun forgotPassword(view: android.view.View) {
        var intent=Intent(this,SecureQueActivity::class.java)
        intent.putExtra("ScreenType","ForgotPassword")
        startActivity(intent)
    }


}