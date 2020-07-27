package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.crysxd.octoapp.base.ui.common.AutoBindViewHolder
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.android.synthetic.main.list_item_file.*
import kotlinx.android.synthetic.main.list_item_title.view.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*

class SelectFileAdapter(private val callback: (FileObject) -> Unit) : RecyclerView.Adapter<SelectFileAdapter.ViewHolder>() {

    var items: List<DataItem> = emptyList()

    var title: String = ""
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    private var picasso: Picasso? = null

    private val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    fun setFiles(files: List<FileObject>) {
        // Sort files by date, folders by name
        val groups = files.groupBy { it::class.java }
        val sortedFiles = groups[FileObject.File::class.java]?.sortedByDescending { (it as FileObject.File).date } ?: emptyList()
        val sortedFolders = groups[FileObject.Folder::class.java]?.sortedBy { it.display } ?: emptyList()

        // Headers
        val headers = mutableListOf<DataItem>()
        if (files.none { !(it as? FileObject.File)?.thumbnail.isNullOrBlank() }) {
            headers.add(DataItem.ThumbnailHint)
        }
        headers.add(DataItem.TitleItem)

        items = listOf(
            headers,
            sortedFolders.map { DataItem.FileItem(it) },
            sortedFiles.map { DataItem.FileItem(it) }
        ).flatten()

        notifyDataSetChanged()
    }

    fun updatePicasso(picasso: Picasso?) {
        this.picasso = picasso
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        DataItem.TitleItem -> VIEW_TYPE_TITLE
        DataItem.ThumbnailHint -> VIEW_TYPE_THUMBNAIL_HINT
        else -> VIEW_TYPE_FILE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_TITLE -> ViewHolder.TitleViewHolder(parent)
        VIEW_TYPE_THUMBNAIL_HINT -> ViewHolder.ThumbnailHintViewHolder(parent)
        VIEW_TYPE_FILE -> ViewHolder.FileViewHolder(parent)
        else -> throw RuntimeException("Unsupported view type $viewType")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = when (holder) {
        is ViewHolder.FileViewHolder -> {
            val file = (items[position] as DataItem.FileItem).file
            holder.textViewTitle.text = file.display

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

                    val iconRes = if (file.typePath.contains(FileObject.FILE_TYPE_MACHINE_CODE)) {
                        R.drawable.ic_outline_print_24
                    } else {
                        R.drawable.ic_outline_insert_drive_file_24
                    }

                    when {
                        picasso == null -> {
                            holder.imageViewFileIcon.setImageResource(iconRes)
                            null
                        }
                        !file.thumbnail.isNullOrBlank() -> picasso?.load(file.thumbnail)?.error(iconRes)?.into(holder.imageViewFileIcon)
                        else -> {
                            // Use Picasso as well to prevent the recycled view to get corrupted
                            // Picasso fails to load the image (as it is vector) so let's set it manually as well
                            picasso?.load(iconRes)?.into(holder.imageViewFileIcon)
                            holder.imageViewFileIcon.setImageResource(iconRes)
                        }
                    }
                }

                else -> Unit

            }.let {}

            holder.itemView.setOnClickListener {
                callback(file)
            }
        }

        is ViewHolder.TitleViewHolder -> {
            holder.itemView.textViewTitle.text = title
            holder.itemView.isVisible = !title.isBlank()
        }

        is ViewHolder.ThumbnailHintViewHolder -> Unit

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
        const val VIEW_TYPE_THUMBNAIL_HINT = 2
    }

    sealed class DataItem {
        data class FileItem(val file: FileObject) : DataItem()
        object TitleItem : DataItem()
        object ThumbnailHint : DataItem()
    }

    sealed class ViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : AutoBindViewHolder(parent, layout) {
        class FileViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_file)
        class TitleViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_title)
        class ThumbnailHintViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_thumbnail_hint)
    }
}