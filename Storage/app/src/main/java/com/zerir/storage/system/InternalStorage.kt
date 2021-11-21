package com.zerir.storage.system

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class InternalStorage(context: Context) {

    private val context = context.applicationContext

    suspend fun delete(filename: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.deleteFile(filename)
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun save(filename: String, bitmap: Bitmap?): Boolean {
        return withContext(Dispatchers.IO) {
            val result = runCatching {
                context.openFileOutput("$filename.jpg", ComponentActivity.MODE_PRIVATE)
                    .use { stream ->
                        val compressed = bitmap?.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                        if (compressed == null || !compressed) {
                            throw IOException("Couldn't save bitmap.")
                        }
                        compressed
                    }
            }
            if (result.isFailure) result.exceptionOrNull()?.printStackTrace()
            result.isSuccess
        }
    }

    suspend fun save(filename: String, uri: Uri?): Boolean {
        if (uri == null) return false
        return withContext(Dispatchers.IO) {
            val result = runCatching {
                val bitmap = context.contentResolver.openInputStream(uri).use { input ->
                    BitmapFactory.decodeStream(input)
                }
                context.openFileOutput("$filename.jpg", ComponentActivity.MODE_PRIVATE)
                    .use { stream ->
                        val compressed = bitmap?.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                        if (compressed == null || !compressed) {
                            throw IOException("Couldn't save bitmap.")
                        }
                        compressed
                    }
            }
            if (result.isFailure) result.exceptionOrNull()?.printStackTrace()
            result.isSuccess
        }
    }

    suspend fun loadAll(converter: InternalConverter): List<StoragePhoto.InternalStoragePhoto> {
        return withContext(Dispatchers.IO) {
            context.filesDir.listFiles { file ->
                file?.let { noNullFile ->
                    (noNullFile.canRead() && noNullFile.isFile) &&
                            (noNullFile.path.endsWith(".jpg") || noNullFile.path.endsWith(".jpeg"))
                } ?: false
            }?.map { filteredFile ->
                converter.convert(filteredFile)
            } ?: listOf()
        }
    }
}