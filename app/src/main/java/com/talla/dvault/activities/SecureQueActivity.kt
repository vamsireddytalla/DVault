package com.talla.dvault.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.talla.dvault.R
import com.talla.dvault.adapters.SpinnerAdapter
import com.talla.dvault.databinding.ActivitySecureQueBinding

class SecureQueActivity : AppCompatActivity()
{
    private lateinit var binding:ActivitySecureQueBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding= ActivitySecureQueBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        var selectedItem=binding.spinner.selectedItem.toString().trim()
        var ans=binding.answer.text.toString().trim()

        if (selectedItem.isBlank() || ans.isBlank())
        {
            Toast.makeText(this, "Enter Answer", Toast.LENGTH_SHORT).show()
            binding.answer.setError("Empty")
            binding.answer.requestFocus()
            return
        }

        var intent=Intent(this,AppLockActivity::class.java)
        startActivity(intent)
    }

}