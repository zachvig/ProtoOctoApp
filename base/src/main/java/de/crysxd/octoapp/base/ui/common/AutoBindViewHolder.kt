package de.crysxd.octoapp.base.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer

abstract class AutoBindViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)),
    LayoutContainer {

    override val containerView = itemView

}