package com.krs.smart.utils

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.krs.smart.R

fun Drawable.applyTint(context: Context): Drawable {
    val mIconColor = ContextCompat.getColor(context, R.color.screen_title_color)
    this.colorFilter = PorterDuffColorFilter(mIconColor, PorterDuff.Mode.SRC_IN)
    return this
}