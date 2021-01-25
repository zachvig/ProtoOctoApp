package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.ui.common.AutoBindViewHolder
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.android.synthetic.main.list_item_error.view.*
import kotlinx.android.synthetic.main.list_item_file.*
import kotlinx.android.synthetic.main.list_item_no_files.view.*
import kotlinx.android.synthetic.main.list_item_thumbnail_hint.view.*
import kotlinx.android.synthetic.main.list_item_title.view.*
import java.text.DateFormat
import java.util.*

class SelectFileAdapter(
    private val onFileSelected: (FileObject) -> Unit,
    private val onHideThumbnailHint: (SelectFileAdapter) -> Unit,
    private val onShowThumbnailInfo: (SelectFileAdapter) -> Unit,
    private val onRetry: (SelectFileAdapter) -> Unit
) : RecyclerView.Adapter<SelectFileAdapter.ViewHolder>() {

    var items: List<DataItem> = emptyList()
    var picasso: Picasso? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private val iconTintColorRes = R.color.primary
    private var folderIcon: Drawable? = null
    private var printableFileIcon: Drawable? = null
    private var otherFileIcon: Drawable? = null

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
            sortedFolders.map { DataItem.File(it) as DataItem }.let {
                if (it.isNotEmpty()) {
                    it.toMutableList().also { it.add(DataItem.Margin) }
                } else {
                    it
                }
            },
            sortedFiles.map
            { DataItem.File(it) }
        ).flatten()

        notifyDataSetChanged()
    }

    override fun getItemId(position: Int) = items[position].hashCode().toLong()

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is DataItem.Title -> VIEW_TYPE_TITLE
        DataItem.ThumbnailHint -> VIEW_TYPE_THUMBNAIL_HINT
        is DataItem.NoFiles -> VIEW_TYPE_NO_FILES
        is DataItem.File -> VIEW_TYPE_FILE
        is DataItem.Error -> VIEW_TYPE_ERROR
        is DataItem.Loading -> VIEW_TYPE_LOADING
        is DataItem.Margin -> VIEW_TYPE_MARGIN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_TITLE -> ViewHolder.TitleViewHolder(parent)
        VIEW_TYPE_THUMBNAIL_HINT -> ViewHolder.ThumbnailHintViewHolder(parent)
        VIEW_TYPE_FILE -> ViewHolder.FileViewHolder(parent)
        VIEW_TYPE_NO_FILES -> ViewHolder.NoFilesViewHolder(parent)
        VIEW_TYPE_ERROR -> ViewHolder.ErrorViewHolder(parent)
        VIEW_TYPE_LOADING -> ViewHolder.LoadingViewHolder(parent)
        VIEW_TYPE_MARGIN -> ViewHolder.MarginViewHolder(parent)
        else -> throw RuntimeException("Unsupported view type $viewType")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = when (holder) {
        is ViewHolder.FileViewHolder -> {
            // Load icons (once)
            val context = holder.itemView.context
            val iconTintColor = ContextCompat.getColor(context, iconTintColorRes)
            if (folderIcon == null || printableFileIcon == null || otherFileIcon == null) {
                val colorFilter = PorterDuffColorFilter(iconTintColor, PorterDuff.Mode.SRC_IN)
                folderIcon = ContextCompat.getDrawable(context, R.drawable.ic_outline_folder_24).also {
                    it?.colorFilter = colorFilter
                }
                printableFileIcon = ContextCompat.getDrawable(context, R.drawable.ic_outline_print_24).also {
                    it?.colorFilter = colorFilter
                }
                otherFileIcon = ContextCompat.getDrawable(context, R.drawable.ic_outline_insert_drive_file_24).also {
                    it?.colorFilter = colorFilter
                }
            }

            val file = (items[position] as DataItem.File).file
            holder.textViewTitle.text = file.display

            when (file) {
                is FileObject.Folder -> {
                    holder.textViewDetail.isVisible = false
                    holder.imageViewArrow.visibility = View.VISIBLE
                    holder.imageViewFileIcon.setImageDrawable(folderIcon)
                }

                is FileObject.File -> {
                    holder.textViewDetail.text = holder.itemView.context.getString(
                        R.string.x_y,
                        dateTimeFormat.format(Date(file.date * 1000)),
                        file.size.asStyleFileSize()
                    )
                    holder.imageViewArrow.visibility = View.GONE
                    holder.textViewDetail.isVisible = true

                    val icon = if (file.typePath?.contains(FileObject.FILE_TYPE_MACHINE_CODE) == true) {
                        printableFileIcon
                    } else {
                        otherFileIcon
                    } ?: ColorDrawable(Color.TRANSPARENT)

                    when {
                        picasso == null -> {
                            holder.imageViewFileIcon.setImageDrawable(icon)
                            null
                        }
                        !file.thumbnail.isNullOrBlank() -> picasso?.load(file.thumbnail)?.error(icon)?.into(holder.imageViewFileIcon)
                        else -> {
                            // Use Picasso as well to prevent the recycled view to get corrupted
                            // Picasso fails to load the image (as it is an empty path) so let's set it manually as well
                            picasso?.cancelRequest(holder.imageViewFileIcon)
                            holder.imageViewFileIcon.setImageDrawable(icon)
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
            holder.itemView.tutorial.onHideAction = { onHideThumbnailHint(this) }
            holder.itemView.tutorial.onLearnMoreAction = { onShowThumbnailInfo(this) }
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

        is ViewHolder.MarginViewHolder -> Unit

    }

    companion object {
        const val VIEW_TYPE_TITLE = 0
        const val VIEW_TYPE_FILE = 1
        const val VIEW_TYPE_THUMBNAIL_HINT = 2
        const val VIEW_TYPE_NO_FILES = 3
        const val VIEW_TYPE_ERROR = 4
        const val VIEW_TYPE_LOADING = 5
        const val VIEW_TYPE_MARGIN = 6
    }

    sealed class DataItem {
        data class File(val file: FileObject) : DataItem()
        data class Title(val title: String?) : DataItem()
        data class NoFiles(val folderName: String?) : DataItem()
        object Error : DataItem()
        object ThumbnailHint : DataItem()
        object Loading : DataItem()
        object Margin : DataItem()
    }

    sealed class ViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : AutoBindViewHolder(parent, layout) {
        class FileViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_file)
        class TitleViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_title)
        class ThumbnailHintViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_thumbnail_hint)
        class NoFilesViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_no_files)
        class ErrorViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_error)
        class LoadingViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_loading)
        class MarginViewHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.list_item_margin)
    }
}