package com.krs.smart.adapters

import android.annotation.SuppressLint
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.krs.smart.room.model.ScaleType
import com.krs.smart.utils.getImgResourceId

@BindingAdapter("avatar")
fun loadImage(imageView: ImageView, scaleType: ScaleType) {
    scaleType.let { imageView.setImageResource(it.getImgResourceId()) }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("scaleTitle")
fun setText(textView: TextView, title: String?) {
    title?.let {textView.text = title}
}