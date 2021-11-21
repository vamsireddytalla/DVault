package com.talla.dvault.activities

import android.content.ActivityNotFoundException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.talla.dvault.databinding.ActivityAboutBinding
import android.content.Intent
import android.net.Uri
import com.talla.dvault.R
import android.widget.Toast





class AboutActivity : AppCompatActivity()
{
    private lateinit var binding:ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.backbtn.setOnClickListener {
            finish()
        }

        binding.contactUs.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", this.resources.getString(R.string.customer_Care), null))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Help")
            startActivity(Intent.createChooser(emailIntent, null))
        }

        binding.rateUs.setOnClickListener {
            val uri = Uri.parse("market://details?id=" + this.packageName)
            val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(myAppLinkToMarket)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, " Sorry, Not able to open!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.privacyPolicy.setOnClickListener {
            val url = this.resources.getString(R.string.privacy_policy)
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

    }

}