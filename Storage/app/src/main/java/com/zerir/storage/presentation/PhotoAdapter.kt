package com.zerir.storage.presentation

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zerir.storage.R
import com.zerir.storage.system.StoragePhoto
import java.lang.IllegalArgumentException

class PhotoAdapter(
    private val listenOnInternal: OnItemClickListener<StoragePhoto.InternalStoragePhoto>? = null,
    private val listenOnExternal: OnItemClickListener<StoragePhoto.ExternalStoragePhoto>? = null,
) : ListAdapter<StoragePhoto, RecyclerView.ViewHolder>(PhotoDiffUtils()) {

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is StoragePhoto.Title -> R.layout.row_title_item
            is StoragePhoto.InternalStoragePhoto -> R.layout.row_internal_photo_item
            is StoragePhoto.ExternalStoragePhoto -> R.layout.row_external_photo_item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            R.layout.row_title_item -> ViewHolder.TitleViewHolder.from(parent)
            R.layout.row_internal_photo_item -> ViewHolder.InternalViewHolder.from(parent)
            R.layout.row_external_photo_item -> ViewHolder.ExternalViewHolder.from(parent)
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.TitleViewHolder -> {
                val item = getItem(position) as StoragePhoto.Title
                holder.bind(item)
            }
            is ViewHolder.InternalViewHolder -> {
                val item = getItem(position) as StoragePhoto.InternalStoragePhoto
                holder.bind(item, listenOnInternal)
            }
            is ViewHolder.ExternalViewHolder -> {
                val item = getItem(position) as StoragePhoto.ExternalStoragePhoto
                holder.bind(item, listenOnExternal)
            }
        }
    }

}

class PhotoDiffUtils : DiffUtil.ItemCallback<StoragePhoto>() {

    override fun areItemsTheSame(oldItem: StoragePhoto, newItem: StoragePhoto): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: StoragePhoto, newItem: StoragePhoto): Boolean {
        return oldItem.name == newItem.name
    }

}