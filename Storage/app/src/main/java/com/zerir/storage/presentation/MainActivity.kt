package com.zerir.storage.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.zerir.storage.R
import com.zerir.storage.databinding.ActivityMainBinding
import com.zerir.storage.system.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var readPermissionGranted = false
    private var writePermissionGranted = false

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    private var isPrivate = false

    private var deletedImageUri: Uri? = null

    private lateinit var ui: Ui

    private val adapter = PhotoAdapter(
        listenOnInternal = object: OnItemClickListener<StoragePhoto.InternalStoragePhoto> {
            override fun onItemClicked(item: StoragePhoto.InternalStoragePhoto) {
                lifecycleScope.launch {
                    internalStorage.delete(item.name)
                    ui.privatePhotos = internalStorage.loadAll(internalConverter)
                    updateList()
                }
            }
        },
        listenOnExternal = object: OnItemClickListener<StoragePhoto.ExternalStoragePhoto> {
            override fun onItemClicked(item: StoragePhoto.ExternalStoragePhoto) {
                lifecycleScope.launch {
                    externalStorage.delete(item.uri, intentSenderLauncher)
                    deletedImageUri = item.uri
                }
            }
        }
    )

    private val contentObserver by lazy {
        object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                if (readPermissionGranted) {
                    lifecycleScope.launch {
                        ui.sharedPhotos = externalStorage.loadAll(externalConverter)
                        updateList()
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
        ExternalStorage(contentResolver)
    }

    private val externalConverter by lazy {
        ExternalConverter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        ui = Ui(this)

        binding.adapter = adapter

        lifecycleScope.launch {
            ui.privatePhotos = internalStorage.loadAll(internalConverter)
            ui.sharedPhotos = externalStorage.loadAll(externalConverter)
            updateList()
        }

        val layoutManager = GridLayoutManager(this, 4)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.currentList[position]) {
                    is StoragePhoto.Title -> 4
                    else -> 1
                }
            }
        }
        binding.photoRv.layoutManager = layoutManager

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )

        val takePhoto =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
                lifecycleScope.launch {
                    val savedSuccessfully = if (isPrivate) {
                        internalStorage.save(UUID.randomUUID().toString(), bitmap)
                    } else {
                        if (writePermissionGranted)
                            externalStorage.save(UUID.randomUUID().toString(), bitmap)
                        else false
                    }
                    if (savedSuccessfully) {
                        if (isPrivate) {
                            ui.privatePhotos = internalStorage.loadAll(internalConverter)
                            updateList()
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
                        ui.sharedPhotos = externalStorage.loadAll(externalConverter)
                        updateList()
                    }
                else
                    Toast.makeText(this, "Can't load External photos", Toast.LENGTH_LONG).show()
            }

        intentSenderLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    if (isSdk29()) {
                        lifecycleScope.launch {
                            externalStorage.delete(
                                deletedImageUri ?: return@launch,
                                intentSenderLauncher
                            )
                        }
                    }
                } else {
                    Toast.makeText(this, "Can't access delete permission", Toast.LENGTH_LONG).show()
                }
            }

        val openGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            lifecycleScope.launch {
                val savedSuccessfully = internalStorage.save(UUID.randomUUID().toString(), uri)
                if (savedSuccessfully) {
                    ui.privatePhotos = internalStorage.loadAll(internalConverter)
                    updateList()
                }
            }
        }

        binding.cameraBt.setOnClickListener { takePhoto.launch() }

        binding.galleryBt.setOnClickListener { openGallery.launch("image/*") }

        binding.privateSwitch.setOnCheckedChangeListener { _, isChecked ->
            isPrivate = isChecked
        }

    }

    private fun updateList() {
        val list = ArrayList<StoragePhoto>()
        list.add(ui.privateTitle)
        list.addAll(ui.privatePhotos)
        list.add(ui.sharedTitle)
        list.addAll(ui.sharedPhotos)
        adapter.submitList(list)
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