package com.dalyly

import android.content.Context
import android.widget.Toast
import android.os.Handler
import android.os.Looper

/**
 * Thread-safe Context extension helper to display Toast messages easily.
 */
fun Context.showToastMessage(message: String, isLong: Boolean = false) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, message, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}
