package com.zerir.storage.system

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class InternalStorage(private val context: Context) {

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

    suspend fun save(filename: String, bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
             val result = runCatching {
                 context.openFileOutput("$filename.jpg", ComponentActivity.MODE_PRIVATE)
                     .use { stream ->
                         val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                         if (!compressed) {
                             throw IOException("Couldn't save bitmap.")
                         }
                         compressed
                     }
            }
            if(result.isFailure) result.exceptionOrNull()?.printStackTrace()
            result.isSuccess
        }
    }

    suspend fun loadAll(converter: InternalConverter): List<InternalStoragePhoto> {
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