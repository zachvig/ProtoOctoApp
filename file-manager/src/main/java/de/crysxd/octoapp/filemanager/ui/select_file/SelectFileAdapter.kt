package de.crysxd.octoapp.filemanager.ui.select_file

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.squareup.picasso.Picasso
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.ext.format
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.databinding.ListItemErrorBinding
import de.crysxd.octoapp.filemanager.databinding.ListItemFileBinding
import de.crysxd.octoapp.filemanager.databinding.ListItemLoadingBinding
import de.crysxd.octoapp.filemanager.databinding.ListItemMarginBinding
import de.crysxd.octoapp.filemanager.databinding.ListItemNoFilesBinding
import de.crysxd.octoapp.filemanager.databinding.ListItemThumbnailHintBinding
import de.crysxd.octoapp.filemanager.databinding.ListItemTitleBinding
import de.crysxd.octoapp.octoprint.models.files.FileObject
import java.util.Date

class SelectFileAdapter(
    private val onFileSelected: (FileObject) -> Unit,
    private val onFileMenuOpened: (FileObject) -> Unit,
    private val onHideThumbnailHint: (SelectFileAdapter) -> Unit,
    private val onShowThumbnailInfo: (SelectFileAdapter) -> Unit,
    private val onRetry: (SelectFileAdapter) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    @SuppressLint("NotifyDataSetChanged")
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = when (holder) {
        is ViewHolder.FileViewHolder -> {
            // Load icons (once)
            val context = holder.itemView.context
            val iconTintColor = ContextCompat.getColor(context, iconTintColorRes)
            if (folderIcon == null || printableFileIcon == null || otherFileIcon == null) {
                val colorFilter = PorterDuffColorFilter(iconTintColor, PorterDuff.Mode.SRC_IN)
                folderIcon = ContextCompat.getDrawable(context, R.drawable.ic_round_folder_24).also {
                    it?.colorFilter = colorFilter
                }
                printableFileIcon = ContextCompat.getDrawable(context, R.drawable.ic_round_print_24).also {
                    it?.colorFilter = colorFilter
                }
                otherFileIcon = ContextCompat.getDrawable(context, R.drawable.ic_round_insert_drive_file_24).also {
                    it?.colorFilter = colorFilter
                }
            }

            val file = (items[position] as DataItem.File).file
            holder.binding.textViewTitle.text = file.display

            when (file) {
                is FileObject.Folder -> {
                    holder.binding.textViewDetail.isVisible = false
                    holder.binding.imageViewArrow.visibility = View.VISIBLE
                    holder.binding.imageViewFileIcon.setImageDrawable(folderIcon)
                }

                is FileObject.File -> {
                    holder.binding.textViewDetail.text = holder.itemView.context.getString(
                        R.string.x_y,
                        Date(file.date * 1000).format(),
                        file.size.asStyleFileSize()
                    )
                    holder.binding.imageViewArrow.visibility = View.GONE
                    holder.binding.textViewDetail.isVisible = true

                    val icon = if (file.typePath?.contains(FileObject.FILE_TYPE_MACHINE_CODE) == true) {
                        printableFileIcon
                    } else {
                        otherFileIcon
                    } ?: ColorDrawable(Color.TRANSPARENT)

                    val resultIcon = when (file.prints?.last?.success) {
                        true -> R.drawable.ic_round_check_circle_24
                        false -> R.drawable.ic_round_highlight_off_circle_24
                        null -> null
                    }
                    resultIcon?.let(holder.binding.resultIndicator::setImageResource)
                    holder.binding.resultIndicator.alpha = if (resultIcon != null) 1f else 0f

                    when {
                        picasso == null -> {
                            holder.binding.imageViewFileIcon.setImageDrawable(icon)
                            null
                        }
                        !file.thumbnail.isNullOrBlank() -> picasso?.load(file.thumbnail)?.error(icon)?.into(holder.binding.imageViewFileIcon)
                        else -> {
                            // Use Picasso as well to prevent the recycled view to get corrupted
                            // Picasso fails to load the image (as it is an empty path) so let's set it manually as well
                            picasso?.cancelRequest(holder.binding.imageViewFileIcon)
                            holder.binding.imageViewFileIcon.setImageDrawable(icon)
                        }
                    }
                }

                else -> Unit

            }.let {}

            holder.itemView.setOnClickListener {
                onFileSelected(file)
            }
            holder.itemView.setOnLongClickListener {
                onFileMenuOpened(file)
                true
            }
        }

        is ViewHolder.TitleViewHolder -> {
            holder.binding.textViewTitle.text = (items[position] as DataItem.Title).title
                ?: holder.itemView.context.getString(R.string.select_file_to_print)
        }

        is ViewHolder.ThumbnailHintViewHolder -> {
            holder.binding.tutorial.onHideAction = { onHideThumbnailHint(this) }
            holder.binding.tutorial.onLearnMoreAction = { onShowThumbnailInfo(this) }
        }

        is ViewHolder.NoFilesViewHolder -> {
            (items[position] as DataItem.NoFiles).folderName?.let {
                holder.binding.textViewNoFilesTitle.text = it
                holder.binding.textViewNoFilesSubitle.text = holder.itemView.context.getString(R.string.this_folder_contains_no_files)
            } ?: run {
                holder.binding.textViewNoFilesTitle.text = holder.itemView.context.getString(R.string.no_files_on_octoprint_title)
                holder.binding.textViewNoFilesSubitle.movementMethod = LinkMovementMethod()
                holder.binding.textViewNoFilesSubitle.text = HtmlCompat.fromHtml(
                    holder.itemView.context.getString(R.string.no_files_on_octoprint_subtitle),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            }
        }

        is ViewHolder.ErrorViewHolder -> holder.binding.buttonRery.setOnClickListener { onRetry(this) }

        is ViewHolder.LoadingViewHolder -> Unit

        is ViewHolder.MarginViewHolder -> Unit

        else -> Unit
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

    sealed class ViewHolder<T : ViewBinding>(binding: T) : ViewBindingHolder<T>(binding) {
        class FileViewHolder(parent: ViewGroup) : ViewHolder<ListItemFileBinding>(
            ListItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        class TitleViewHolder(parent: ViewGroup) : ViewHolder<ListItemTitleBinding>(
            ListItemTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        class ThumbnailHintViewHolder(parent: ViewGroup) : ViewHolder<ListItemThumbnailHintBinding>(
            ListItemThumbnailHintBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        class NoFilesViewHolder(parent: ViewGroup) : ViewHolder<ListItemNoFilesBinding>(
            ListItemNoFilesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        class ErrorViewHolder(parent: ViewGroup) : ViewHolder<ListItemErrorBinding>(
            ListItemErrorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        class LoadingViewHolder(parent: ViewGroup) : ViewHolder<ListItemLoadingBinding>(
            ListItemLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        class MarginViewHolder(parent: ViewGroup) : ViewHolder<ListItemMarginBinding>(
            ListItemMarginBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }
}