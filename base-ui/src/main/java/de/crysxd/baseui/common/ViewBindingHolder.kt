package de.crysxd.baseui.common

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class ViewBindingHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)