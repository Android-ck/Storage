package com.zerir.storage.presentation

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("uri")
fun ImageView.loadImage(uri: Uri?) {
    Glide.with(context)
        .load(uri)
        .into(this)
}

@BindingAdapter("bitmap")
fun ImageView.loadImage(bitmap: Bitmap?) {
    Glide.with(context)
        .load(bitmap)
        .into(this)
}