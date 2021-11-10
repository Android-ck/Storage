package com.zerir.storage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.runtime.Composable
import com.zerir.storage.ui.theme.StorageTheme
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.zerir.storage.system.*
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {

    private var readPermissionGranted = false
    private var writePermissionGranted = false

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val privateList = mutableStateOf<List<InternalStoragePhoto>>(listOf())
    private val sharedList = mutableStateOf<List<ExternalStoragePhoto>>(listOf())

    private val isPrivate = mutableStateOf(false)

    private var deletedImageUri: Uri? = null

    private val contentObserver by lazy {
        object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                if (readPermissionGranted) {
                    lifecycleScope.launch {
                        sharedList.value = externalStorage.loadAll(externalConverter)
                    }
                }
            }
        }
    }

    private val internalStorage by lazy {
        InternalStorage(applicationContext)
    }

    private val internalConverter by lazy {
        InternalConverter()
    }

    private val externalStorage by lazy {
        ExternalStorage(applicationContext)
    }

    private val externalConverter by lazy {
        ExternalConverter()
    }

    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )

        val takePhoto =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
                lifecycleScope.launch {
                    val savedSuccessfully = if (isPrivate.value) {
                        internalStorage.save(UUID.randomUUID().toString(), bitmap)
                    } else {
                        if (writePermissionGranted)
                            externalStorage.save(UUID.randomUUID().toString(), bitmap)
                        else false
                    }
                    if (savedSuccessfully) {
                        if (isPrivate.value) {
                            privateList.value = internalStorage.loadAll(internalConverter)
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to save photo", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }

        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                readPermissionGranted =
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
                writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                    ?: writePermissionGranted

                if (readPermissionGranted)
                    lifecycleScope.launch {
                        sharedList.value = externalStorage.loadAll(externalConverter)
                    }
                else
                    Toast.makeText(this, "Can't load External photos", Toast.LENGTH_LONG).show()
            }

        intentSenderLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    if (isSdk29()) {
                        lifecycleScope.launch {
                            externalStorage.delete(deletedImageUri ?: return@launch, intentSenderLauncher)
                        }
                    }
                } else {
                    Toast.makeText(this, "Can't access delete permission", Toast.LENGTH_LONG).show()
                }
            }

        setContent {
            val composableScope = rememberCoroutineScope()
            composableScope.launch {
                privateList.value = internalStorage.loadAll(internalConverter)
                if (readPermissionGranted)
                    lifecycleScope.launch {
                        sharedList.value = externalStorage.loadAll(externalConverter)
                    }
                else
                    Toast.makeText(
                        this@MainActivity,
                        "Can't load External photos",
                        Toast.LENGTH_LONG
                    ).show()
            }

            StorageTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, bottom = 18.dp),
                        ) {
                            Button(onClick = {
                                takePhoto.launch()
                            }) {
                                Text(text = "Take Photo")
                            }
                            RadioButton(selected = isPrivate.value, onClick = {
                                isPrivate.value = !isPrivate.value
                            })
                        }
                        Text(text = "Private Photos")
                        PhotoGridInternal(
                            photos = privateList.value,
                            modifier = Modifier.fillMaxWidth(),
                            onLongClick = { photo ->
                                composableScope.launch {
                                    internalStorage.delete(photo.name)
                                    privateList.value = internalStorage.loadAll(internalConverter)
                                }
                            },
                        )
                        Text(text = "Shared Photos")
                        PhotoGridExternal(
                            context = this@MainActivity,
                            photos = sharedList.value,
                            modifier = Modifier.fillMaxWidth(),
                            onLongClick = { photo ->
                                composableScope.launch {
                                    externalStorage.delete(photo.uri, intentSenderLauncher)
                                    deletedImageUri = photo.uri
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        val hasReadPermissions = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermissions = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        readPermissionGranted = hasReadPermissions
        writePermissionGranted = hasWritePermissions || isSdk29OrOver()

        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (!readPermissionGranted)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!writePermissionGranted)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissions.isNotEmpty())
            permissionsLauncher.launch(permissions.toTypedArray())
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(contentObserver)
    }

}

@ExperimentalFoundationApi
@Composable
fun PhotoGridInternal(
    photos: List<InternalStoragePhoto>,
    modifier: Modifier,
    onLongClick: (InternalStoragePhoto) -> Unit
) {
    LazyVerticalGrid(
        cells = GridCells.Adaptive(minSize = 80.dp),
        modifier = modifier
    ) {
        items(photos) { photo ->
            PhotoItemInternal(photo, onLongClick)
        }
    }
}

@Composable
fun PhotoItemInternal(photo: InternalStoragePhoto, onLongClick: (InternalStoragePhoto) -> Unit) {
    Image(
        bitmap = photo.bitmap.asImageBitmap(),
        contentDescription = "",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .height(120.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick(photo) },
                )
            },
    )
}

@ExperimentalFoundationApi
@Composable
fun PhotoGridExternal(
    context: Context,
    photos: List<ExternalStoragePhoto>,
    modifier: Modifier,
    onLongClick: (ExternalStoragePhoto) -> Unit
) {
    LazyVerticalGrid(
        cells = GridCells.Adaptive(minSize = 80.dp),
        modifier = modifier
    ) {
        items(photos) { photo ->
            PhotoItemExternal(context, photo, onLongClick)
        }
    }
}

@Composable
fun PhotoItemExternal(
    context: Context,
    photo: ExternalStoragePhoto,
    onLongClick: (ExternalStoragePhoto) -> Unit
) {
    val stream = context.contentResolver.openInputStream(photo.uri)
    val bitmap = BitmapFactory.decodeStream(stream)
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .height(120.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick(photo) },
                )
            },
    )
}