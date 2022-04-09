package com.talla.dvault.utills

import android.os.Build

inline fun <T> sdk30AndUp(onSdk30 : () -> T):T?{
    return if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
        onSdk30()
    }else null
}