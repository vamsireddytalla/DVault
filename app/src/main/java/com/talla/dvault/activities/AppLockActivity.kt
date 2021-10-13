package com.talla.dvault.activities


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.talla.dvault.R
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.databinding.ActivityAppLockBinding
import com.talla.dvault.utills.DateUtills
import com.talla.dvault.viewmodels.AppLockViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

private const val TAG = "AppLockActivity"

@AndroidEntryPoint
class AppLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppLockBinding
    private var question:String?=null
    private var answer:String?=null
    private val viewModel: AppLockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bundle = intent.extras
        if (bundle != null) {
            question = bundle.getString(getString(R.string.question))
            answer = bundle.getString(getString(R.string.answer))
        }
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    fun nextBtn(view: android.view.View) {
        val password = binding.password.text.toString().trim()
        val cnf_password = binding.cnfPassword.text.toString().trim()

        if (password.isBlank() || cnf_password.isBlank()) {
            showSnackBar(getString(R.string.fill_both_fields))
            return
        } else if (password != cnf_password) {
            showSnackBar(getString(R.string.both_pass_not_same))
            return
        } else if (password.length<4) {
            showSnackBar(getString(R.string.check_pass_length))
            return
        }
        else {
            var appLockModel=AppLockModel(question!!,answer!!,
                cnf_password,true,
                DateUtills.getSystemTime(this).toString(),this.resources.getString(R.string.app_name))
             runBlocking {
                 val res=viewModel.saveAppLockData(appLockModel)
                 showDialog(getString(R.string.password_set_sucess),getString(R.string.go_to_dashboard))
             }
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

    fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showDialog(title:String,message: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton(getString(R.string.ok)) { dialogInterface, i ->
            dialogInterface.dismiss()
            var intent=Intent(this,DashBoardActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
        alertDialogBuilder.show()
    }

}