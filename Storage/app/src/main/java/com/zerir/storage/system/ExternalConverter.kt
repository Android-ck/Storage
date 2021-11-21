package com.zerir.storage.system

import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExternalConverter {

    suspend fun convert(
        cursor: Cursor,
        idColumn: Int,
        displayNameColumn: Int,
        widthColumn: Int,
        heightColumn: Int,
    ): StoragePhoto.ExternalStoragePhoto? {
        return withContext(Dispatchers.IO) {
            val result = runCatching<StoragePhoto.ExternalStoragePhoto> {
                val id = cursor.getLong(idColumn)
                StoragePhoto.ExternalStoragePhoto(
                    id = id,
                    name = cursor.getString(displayNameColumn),
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn),
                    uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ),
                )
            }
            if(result.isFailure) {
                Log.e("EX convert", "result Failure ${result.exceptionOrNull()?.localizedMessage}")
                result.exceptionOrNull()?.printStackTrace()
            }
            result.getOrNull()
        }
    }

}