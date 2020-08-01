package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.crysxd.octoapp.base.ui.common.AutoBindViewHolder
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.android.synthetic.main.list_item_error.view.*
import kotlinx.android.synthetic.main.list_item_file.*
import kotlinx.android.synthetic.main.list_item_no_files.view.*
import kotlinx.android.synthetic.main.list_item_thumbnail_hint.view.*
import kotlinx.android.synthetic.main.list_item_title.view.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*

class SelectFileAdapter(
    private val onFileSelected: (FileObject) -> Unit,
    private val onHideThumbnailHint: (SelectFileAdapter) -> Unit,
    private val onShowThumbnailInfo: (SelectFileAdapter) -> Unit,
    private val onRetry: (SelectFileAdapter) -> Unit
) : RecyclerView.Adapter<SelectFileAdapter.ViewHolder>() {

    var items: List<DataItem> = emptyList()

    private var picasso: Picasso? = null

    private val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    init {
        setHasStableIds(true)
    }

    fun showLoading() {
        items = listOf(DataItem.Loading)
        notifyDataSetChanged()
    }

    fun showError() {
        items = listOf(DataItem.Error)
        notifyDataSetChanged()
    }

    fun showFiles(folderName: String?, files: List<FileObject>, showThumbnailHint: Boolean) {
        // Sort files by date, folders by name
        val groups = files.groupBy { it::class.java }
        val sortedFiles = groups[FileObject.File::class.java]?.sortedByDescending { (it as FileObject.File).date } ?: emptyList()
        val sortedFolders = groups[FileObject.Folder::class.java]?.sortedBy { it.display } ?: emptyList()

        // Headers
        val headers = mutableListOf<DataItem>()
        if (sortedFiles.isEmpty() && sortedFolders.isEmpty()) {
            headers.add(DataItem.NoFiles(folderName))
        } else {
            if (showThumbnailHint) {
                headers.add(DataItem.ThumbnailHint)
            }
            headers.add(DataItem.Title(folderName))
        }

        items = listOf(
            headers,
            sortedFolders.map { DataItem.File(it) },
            sortedFiles.map { DataItem.File(it) }
        ).flatten()

        notifyDataSetChanged()
    }

    override fun getItemId(position: Int) = items[position].hashCode().toLong()

    fun updatePicasso(picasso: Picasso?) {
        this.picasso = picasso
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is DataItem.Title -> VIEW_TYPE_TITLE
        DataItem.ThumbnailHint -> VIEW_TYPE_THUMBNAIL_HINT
        is DataItem.NoFiles -> VIEW_TYPE_NO_FILES
        is DataItem.File -> VIEW_TYPE_FILE
        is DataItem.Error -> VIEW_TYPE_ERROR
        is DataItem.Loading -> VIEW_TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_TITLE -> ViewHolder.TitleViewHolder(parent)
        VIEW_TYPE_THUMBNAIL_HINT -> ViewHolder.ThumbnailHintViewHolder(parent)
        VIEW_TYPE_FILE -> ViewHolder.FileViewHolder(parent)
        VIEW_TYPE_NO_FILES -> ViewHolder.NoFilesViewHolder(parent)
        VIEW_TYPE_ERROR -> ViewHolder.ErrorViewHolder(parent)
        VIEW_TYPE_LOADING -> ViewHolder.LoadingViewHolder(parent)
        else -> throw RuntimeException("Unsupported view type $viewType")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = when (holder) {
        is ViewHolder.FileViewHolder -> {
            val file = (items[position] as DataItem.File).file
            holder.textViewTitle.text = file.display

            when (file) {
                is FileObject.Folder -> {
                    holder.textViewDetail.text = if ((file.children?.size ?: 0 > 0)) {
                        holder.itemView.context.getString(R.string.x_items_y, file.children?.size, styleFileSize(file.size))
                    } else {
                        holder.itemView.context.getString(R.string.empty)
                    }

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
                onFileSelected(file)
            }
        }

        is ViewHolder.TitleViewHolder -> {
            holder.itemView.textViewTitle.text = (items[position] as DataItem.Title).title
                ?: holder.itemView.context.getString(R.string.select_file_to_print)
        }

        is ViewHolder.ThumbnailHintViewHolder -> {
            holder.itemView.buttonHide.setOnClickListener { onHideThumbnailHint(this) }
            holder.itemView.buttonLearnMore.setOnClickListener { onShowThumbnailInfo(this) }
        }

        is ViewHolder.NoFilesViewHolder -> {
            (items[position] as DataItem.NoFiles).folderName?.let {
                holder.itemView.textViewNoFilesTitle.text = it
                holder.itemView.textViewNoFilesSubitle.text = holder.itemView.context.getString(R.string.this_folder_contains_no_files)
            } ?: run {
                holder.itemView.textViewNoFilesTitle.text = holder.itemView.context.getString(R.string.no_files_on_octoprint_title)
                holder.itemView.textViewNoFilesSubitle.movementMethod = LinkMovementMethod()
                holder.itemView.textViewNoFilesSubitle.text = HtmlCompat.fromHtml(
                    holder.itemView.context.getString(R.string.no_files_on_octoprint_subtitle),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            }
        }

        is ViewHolder.ErrorViewHolder -> holder.itemView.buttonRery.setOnClickListener { onRetry(this) }

        is ViewHolder.LoadingViewHolder -> Unit

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
        const val VIEW_TYPE_NO_FILES = 3
        const val VIEW_TYPE_ERROR = 4
        const val VIEW_TYPE_LOADING = 5
    }

    sealed class DataItem {
        data class File(val file: FileObject) : DataItem()
        data class Title(val title: String?) : DataItem()
        data class NoFiles(val folderName: String?) : DataItem()
        object Error : DataItem()
        object ThumbnailHint : DataItem()
        object Loading : DataItem()
    }

    sealed class ViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : AutoBindViewHolder(parent, layout) {
        class FileViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_file)
        class TitleViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_title)
        class ThumbnailHintViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_thumbnail_hint)
        class NoFilesViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_no_files)
        class ErrorViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_error)
        class LoadingViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_loading)
    }
}