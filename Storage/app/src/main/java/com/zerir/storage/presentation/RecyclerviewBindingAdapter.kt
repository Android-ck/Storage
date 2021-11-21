package com.zerir.storage.presentation

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView

@BindingAdapter(value = ["adapter", "isFixedSize"])
fun RecyclerView.setupAdapter(
    adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?,
    isFixedSize: Boolean? = null,
) {
    this.setHasFixedSize(isFixedSize ?: true)
    this.adapter = adapter
}