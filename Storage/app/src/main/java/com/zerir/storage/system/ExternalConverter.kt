package com.zerir.storage.system

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
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
        context: Context,
    ): ExternalStoragePhoto? {
        return withContext(Dispatchers.IO) {
            val result = runCatching<ExternalStoragePhoto> {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                ExternalStoragePhoto(
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