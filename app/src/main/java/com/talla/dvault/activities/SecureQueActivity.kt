package com.talla.dvault.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.R
import com.talla.dvault.adapters.SpinnerAdapter
import com.talla.dvault.databinding.ActivitySecureQueBinding
import com.talla.dvault.viewmodels.AppLockViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

private const val TAG = "SecureQueActivity"
@AndroidEntryPoint
class SecureQueActivity : AppCompatActivity()
{
    private lateinit var binding:ActivitySecureQueBinding
    private var screenType:String?=null
    private val viewModel:AppLockViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding= ActivitySecureQueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var bundle: Bundle? =intent.extras
        if (bundle!=null)
        {
           screenType = bundle.getString("ScreenType")
        }

        var customAdapter=SpinnerAdapter(this.resources.getStringArray(R.array.security_questions).asList(),this)
        binding.spinner.adapter=customAdapter

        binding.backBtn.setOnClickListener {
            finish()
        }

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

    fun next(view: android.view.View) {
        var quesItem=binding.spinner.selectedItem.toString().trim()
        var ans=binding.answer.text.toString().trim()

        if (quesItem.isBlank() || ans.isBlank())
        {
            Toast.makeText(this, "Enter Answer", Toast.LENGTH_SHORT).show()
            binding.answer.setError("Empty")
            binding.answer.requestFocus()
            return
        }else if (ans.length<4)
        {
            showSnackBar("Answer Length Atleast 4 characters")
            return
        }
        if (screenType!=null)
        {
            runBlocking {
                var result=viewModel.checkQuestionAndAns(quesItem,ans)
                if (result>0)
                {
                    openAppLockActivity(quesItem, ans)
                }else{
                    showSnackBar("Invalid Credentials")
                }
            }
        }else{
           openAppLockActivity(quesItem, ans)
        }

    }

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    fun openAppLockActivity(quesItem:String,ans:String)
    {
        var intent=Intent(this@SecureQueActivity,AppLockActivity::class.java)
        intent.putExtra(getString(R.string.question),quesItem)
        intent.putExtra(getString(R.string.answer),ans)
        startActivity(intent)
    }


}