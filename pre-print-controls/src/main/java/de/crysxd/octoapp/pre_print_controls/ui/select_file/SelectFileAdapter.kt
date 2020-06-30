package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.ui.common.AutoBindViewHolder
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.android.synthetic.main.list_item_file.*
import kotlinx.android.synthetic.main.list_item_title.view.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*

class SelectFileAdapter(private val callback: (FileObject) -> Unit) : RecyclerView.Adapter<SelectFileAdapter.ViewHolder>() {

    var files: List<FileObject?> = emptyList()
        set(value) {
            // Sort files by date, folders by name
            val groups = value.filterNotNull().groupBy { it::class.java }
            val files = groups[FileObject.File::class.java]?.sortedByDescending { (it as FileObject.File).date } ?: emptyList()
            val folders = groups[FileObject.Folder::class.java]?.sortedBy { it.display } ?: emptyList()

            field = listOf(listOf(null), folders, files).flatten()
            notifyDataSetChanged()
        }

    var title: String = ""
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    private val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    override fun getItemCount() = files.size

    override fun getItemViewType(position: Int) = when(files[position]) {
        null -> VIEW_TYPE_TITLE
        else -> VIEW_TYPE_FILE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when(viewType) {
        VIEW_TYPE_TITLE -> ViewHolder.TitleViewHolder(parent)
        VIEW_TYPE_FILE -> ViewHolder.FileViewHolder(parent)
        else -> throw RuntimeException("Unsupported view type $viewType")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = when(holder) {
        is ViewHolder.FileViewHolder -> {
            val file = files[position]
            holder.textViewTitle.text = file?.display

            when (file) {
                is FileObject.Folder -> {
                    holder.textViewDetail.text = holder.itemView.context.getString(
                        R.string.x_items_y,
                        file.children?.size,
                        styleFileSize(file.size)
                    )
                    holder.imageView.visibility = View.VISIBLE
                    holder.imageViewFileIcon.setImageResource(R.drawable.ic_outline_folder_24)
                }
                is FileObject.File -> {
                    holder.textViewDetail.text = holder.itemView.context.getString(
                        R.string.x_y,
                        dateTimeFormat.format(Date(file.date * 1000)),
                        styleFileSize(file.size)
                    )
                    holder.imageView.visibility = View.INVISIBLE
                    holder.imageViewFileIcon.setImageResource(
                        if (file.typePath.contains(FileObject.FILE_TYPE_MACHINE_CODE)) {
                            R.drawable.ic_outline_print_24
                        } else {
                            R.drawable.ic_outline_insert_drive_file_24
                        }
                    )
                }
                else -> Unit
            }.let {}

            holder.itemView.setOnClickListener {
                file?.let(callback)
            }
        }

        is ViewHolder.TitleViewHolder -> {
            holder.itemView.textViewTitle.text = title
            holder.itemView.isVisible = !title.isBlank()
        }
    }

    private fun styleFileSize(size: Long): String {
        val kb = size / 1024f
        val mb = kb / 1024f
        val gb = mb / 1024f

        return when {
            gb > 1 -> DecimalFormat("#.## 'GiB'").format(gb)
            mb > 1 -> DecimalFormat("#.# 'MiB'").format(mb)
            else -> DecimalFormat("# 'kiB'").format(kb)
        }
    }

    companion object {
        const val VIEW_TYPE_TITLE = 0
        const val VIEW_TYPE_FILE = 1
    }

    sealed class ViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : AutoBindViewHolder(parent, layout) {
        class FileViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_file)
        class TitleViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_title)
    }
}