package com.zerir.storage.system

import android.graphics.Bitmap
import android.net.Uri

data class InternalStoragePhoto(
    val name: String,
    val bitmap: Bitmap,
)

data class ExternalStoragePhoto(
    val id: Long,
    val name: String,
    val width: Int,
    val height: Int,
    val uri: Uri,
)
