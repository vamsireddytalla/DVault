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

        fun getLastBackUpTime(context: Context): String? {
            val date = Date(System.currentTimeMillis())
            val pattern = context.resources.getString(R.string.dateFormat)
            val simpleDateFormat = SimpleDateFormat(pattern)
            return simpleDateFormat.format(date)
        }

        fun convertMilToDate(context: Context, milliSec: Long): String? {
            val date = Date(milliSec)
            val pattern = context.resources.getString(R.string.dateFormat)
            val simpleDateFormat = SimpleDateFormat(pattern)
            return simpleDateFormat.format(date)
        }

        fun driveDateToTimeStamp(myDate:String):Long{
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            val date = sdf.parse(myDate)
            val millis = date.time
            return millis
        }

        fun converTimeStampToDate(context: Context,millis:Long): String? {
            val date = millis
            val pattern = context.resources.getString(R.string.dateFormat)
            val simpleDateFormat = SimpleDateFormat(pattern)
            return simpleDateFormat.format(date)
        }

    }

}