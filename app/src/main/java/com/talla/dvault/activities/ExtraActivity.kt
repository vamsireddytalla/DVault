package com.talla.dvault.activities

import android.opengl.Visibility
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import androidx.annotation.RequiresApi
import com.bumptech.glide.RequestManager
import com.talla.dvault.R
import com.talla.dvault.databinding.ActivityExtraBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "ExtraActivity"

@AndroidEntryPoint
class ExtraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExtraBinding
    private lateinit var path: String
    private lateinit var catType: String

    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            path = intent.getStringExtra(this.resources.getString(R.string.key)).toString()
            catType = intent.getStringExtra(this.resources.getString(R.string.key2)).toString()
            Log.d(TAG, "onCreate: $path/$catType")
            if (catType == "Img") {
                openImageView()
            } else if (catType == "Vdo") {
                openVideoView()
            } else {
                finish()
            }
        }
    }

    fun openImageView() {
        binding.imageVieew.visibility = View.VISIBLE
        glide.load(path).into(binding.imageVieew)
    }

    fun openVideoView() {
        binding.videoView.visibility = View.VISIBLE
        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)
        binding.videoView.setVideoPath(path)
        binding.videoView.setMediaController(mediaController)
        binding.videoView.requestFocus()
        binding.videoView.start()
    }


}