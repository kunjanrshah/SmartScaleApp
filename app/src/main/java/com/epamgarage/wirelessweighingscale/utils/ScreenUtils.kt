package com.epamgarage.wirelessweighingscale.utils

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.epamgarage.wirelessweighingscale.R

fun Drawable.applyTint(context: Context): Drawable {
    val mIconColor = ContextCompat.getColor(context, R.color.screen_title_color)
    this.colorFilter = PorterDuffColorFilter(mIconColor, PorterDuff.Mode.SRC_IN)
    return this
}