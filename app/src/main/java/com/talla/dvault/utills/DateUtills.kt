package com.talla.dvault.utills

import android.content.Context
import com.talla.dvault.R
import java.text.SimpleDateFormat
import java.util.*

class DateUtills
{
    companion object
    {
        fun getSystemTime(context: Context): String? {
            val date = Date(System.currentTimeMillis())
            val pattern = context.resources.getString(R.string.date_format)
            val simpleDateFormat = SimpleDateFormat(pattern)
            return simpleDateFormat.format(date)
        }
    }
}