package com.zerir.storage.system

import android.graphics.Bitmap
import android.net.Uri

sealed class StoragePhoto(val name: String) {

    class Title(name: String) : StoragePhoto(name)

    class InternalStoragePhoto(
        name: String,
        val bitmap: Bitmap,
    ) : StoragePhoto(name)

    class ExternalStoragePhoto(
        val id: Long,
        name: String,
        val width: Int,
        val height: Int,
        val uri: Uri,
    ) : StoragePhoto(name)
}
