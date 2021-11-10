package com.zerir.storage.system

import android.graphics.BitmapFactory
import java.io.File

class InternalConverter {

    fun convert(file: File): InternalStoragePhoto {
        val bytes = file.readBytes()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return InternalStoragePhoto(file.name, bitmap)
    }

}