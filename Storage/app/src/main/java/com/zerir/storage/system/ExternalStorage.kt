package com.zerir.storage.system

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class ExternalStorage(private val contentResolver: ContentResolver) {

    suspend fun delete(
        uri: Uri,
        intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                contentResolver.delete(uri, null, null)
                true
            } catch (e: SecurityException) {
                val intentSender = when {
                    isSdk30OrOver() -> {
                        MediaStore.createDeleteRequest(
                            contentResolver,
                            listOf(uri)
                        ).intentSender
                    }
                    isSdk29OrOver() -> {
                        val recoverableSecurityException = e as? RecoverableSecurityException
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    }
                    else -> {
                        e.printStackTrace()
                        null
                    }
                }
                intentSender?.let { sender ->
                    intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    true
                } ?: false
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun save(displayName: String, bitmap: Bitmap?): Boolean {
        return withContext(Dispatchers.IO) {
            val imageCollection = if(isSdk29OrOver()) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.WIDTH, bitmap?.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap?.height)
            }
            val result = runCatching {
                contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                    contentResolver.openOutputStream(uri).use { outputStream ->
                        val compressed = bitmap?.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        if(compressed == null || !compressed) {
                            throw IOException("Couldn't save bitmap")
                        }
                    }
                } ?: throw IOException("Couldn't create MediaStore entry")
            }
            if(result.isFailure) result.exceptionOrNull()?.printStackTrace()
            result.isSuccess
        }
    }

    suspend fun loadAll(converter: ExternalConverter): List<StoragePhoto.ExternalStoragePhoto> {
        return withContext(Dispatchers.IO) {
            val collection = if (isSdk29OrOver()) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
            )

            val photos = mutableListOf<StoragePhoto.ExternalStoragePhoto>()

            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                while (cursor.moveToNext()) {
                    val photo = converter.convert(
                        cursor,
                        idColumn = idColumn,
                        displayNameColumn = displayNameColumn,
                        widthColumn = widthColumn,
                        heightColumn = heightColumn,
                    )
                    photo?.let { photos.add(it) }
                }
                cursor.close()
            }
            photos
        }
    }
}