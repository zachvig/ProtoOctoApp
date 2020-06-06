package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.android.extensions.LayoutContainer

class FileViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.list_item_file, parent, false)
), LayoutContainer {

    override val containerView = itemView

}