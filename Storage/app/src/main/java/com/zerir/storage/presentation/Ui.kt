package com.zerir.storage.presentation

import android.content.Context
import com.zerir.storage.R
import com.zerir.storage.system.StoragePhoto

data class Ui(
    val context: Context,
    val privateTitle: StoragePhoto.Title = StoragePhoto.Title(context.getString(R.string.private_storage)),
    var privatePhotos: List<StoragePhoto.InternalStoragePhoto> = listOf(),
    val sharedTitle: StoragePhoto.Title = StoragePhoto.Title(context.getString(R.string.shared_storage)),
    var sharedPhotos: List<StoragePhoto.ExternalStoragePhoto> = listOf(),
)
