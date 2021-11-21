package com.zerir.storage.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.zerir.storage.databinding.RowExternalPhotoItemBinding
import com.zerir.storage.databinding.RowInternalPhotoItemBinding
import com.zerir.storage.databinding.RowTitleItemBinding
import com.zerir.storage.system.StoragePhoto

sealed class ViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

    class TitleViewHolder(private val binding: RowTitleItemBinding) : ViewHolder(binding) {

        companion object {
            fun from(parent: ViewGroup): TitleViewHolder {
                val context = parent.context
                val layoutInflater = LayoutInflater.from(context)
                val binding = RowTitleItemBinding.inflate(layoutInflater, parent, false)
                return TitleViewHolder(binding)
            }
        }

        fun bind(title: StoragePhoto.Title) {
            binding.title = title
        }

    }

    class InternalViewHolder(private val binding: RowInternalPhotoItemBinding) : ViewHolder(binding) {

        companion object {
            fun from(parent: ViewGroup): InternalViewHolder {
                val context = parent.context
                val layoutInflater = LayoutInflater.from(context)
                val binding = RowInternalPhotoItemBinding.inflate(layoutInflater, parent, false)
                return InternalViewHolder(binding)
            }
        }

        fun bind(item: StoragePhoto.InternalStoragePhoto, onItemClickListener: OnItemClickListener<StoragePhoto.InternalStoragePhoto>?) {
            binding.photo = item

            binding.root.setOnLongClickListener {
                onItemClickListener?.onItemClicked(item)
                true
            }
        }

    }

    class ExternalViewHolder(private val binding: RowExternalPhotoItemBinding) : ViewHolder(binding) {

        companion object {
            fun from(parent: ViewGroup): ExternalViewHolder {
                val context = parent.context
                val layoutInflater = LayoutInflater.from(context)
                val binding = RowExternalPhotoItemBinding.inflate(layoutInflater, parent, false)
                return ExternalViewHolder(binding)
            }
        }

        fun bind(item: StoragePhoto.ExternalStoragePhoto, onItemClickListener: OnItemClickListener<StoragePhoto.ExternalStoragePhoto>?) {
            binding.photo = item

            binding.root.setOnLongClickListener {
                onItemClickListener?.onItemClicked(item)
                true
            }
        }

    }

}

interface OnItemClickListener<T: StoragePhoto> {
    fun onItemClicked(item: T)
}