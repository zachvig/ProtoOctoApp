package de.crysxd.octoapp.filemanager.ui.select_file

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.squareup.picasso.Picasso
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.ext.format
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
    private val context: Context,
    private val onFileSelected: (FileObject) -> Unit,
    private val onFileMenuOpened: (FileObject) -> Unit,
    private val onHideThumbnailHint: (SelectFileAdapter) -> Unit,
    private val onShowThumbnailInfo: (SelectFileAdapter) -> Unit,
    private val onAddItemClicked: () -> Unit,
    private val onRetry: (SelectFileAdapter) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<DataItem> = emptyList()
    var picasso: Picasso? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private var folderIcon: Drawable = loadDrawable(R.drawable.ic_round_folder_24)
    private var printableFileIcon: Drawable = loadDrawable(R.drawable.ic_round_print_24)
    private var otherFileIcon: Drawable = loadDrawable(R.drawable.ic_round_insert_drive_file_24)
    private var uploadIcon: Drawable = loadDrawable(R.drawable.ic_round_upload_24).also {
        it.alpha = it.alpha / 2
    }

    init {
        setHasStableIds(true)
    }

    private fun loadDrawable(@DrawableRes res: Int) = ContextCompat.getDrawable(context, res).also {
        it?.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.accent), PorterDuff.Mode.SRC_IN)
    } ?: ColorDrawable(Color.TRANSPARENT)

    fun showLoading() {
        items = listOf(DataItem.Loading)
        notifyDataSetChanged()
    }

    fun showError() {
        items = listOf(DataItem.Error)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showFiles(folderName: String?, files: List<SelectFileViewModel.FileWrapper>, showThumbnailHint: Boolean) {
        val newItems = files.map {
            when (it) {
                is SelectFileViewModel.FileWrapper.FileObjectWrapper -> DataItem.File(
                    file = it.fileObject,
                    name = it.fileObject.display,
                    detail = when (it.fileObject) {
                        is FileObject.File -> context.getString(R.string.x_y, Date(it.fileObject.date * 1000).format(), it.fileObject.size.asStyleFileSize())
                        is FileObject.Folder -> null
                    },
                    iconUrl = (it.fileObject as? FileObject.File)?.thumbnail,
                    iconPlaceholder = when (it.fileObject) {
                        is FileObject.File -> if (it.fileObject.typePath?.contains(FileObject.FILE_TYPE_MACHINE_CODE) == true) printableFileIcon else otherFileIcon
                        is FileObject.Folder -> folderIcon
                    },
                    resultIcon = when ((it.fileObject as? FileObject.File)?.prints?.last?.success) {
                        null -> null
                        false -> R.drawable.ic_round_highlight_off_circle_24
                        true -> R.drawable.ic_round_check_circle_24
                    },
                    id = (it.fileObject.path + it.fileObject.name).hashCode()
                )

                is SelectFileViewModel.FileWrapper.UploadWrapper -> DataItem.File(
                    file = null,
                    name = it.upload.name,
                    detail = "Uploading...",
                    iconUrl = null,
                    iconPlaceholder = uploadIcon,
                    resultIcon = null,
                    id = it.upload.id.hashCode()
                )
            } as DataItem
        }.toMutableList()

        // Insert spacer between folders and files
        newItems.indexOfLast { it is DataItem.File && it.file is FileObject.Folder }.takeIf { it >= 0 }?.let {
            newItems.add(it + 1, DataItem.Margin)
        }

        // Headers
        if (newItems.isEmpty()) {
            newItems.add(DataItem.NoFiles(folderName))
        } else {
            newItems.add(0, DataItem.Title(folderName))
            if (showThumbnailHint) {
                newItems.add(0, DataItem.ThumbnailHint)
            }
        }

        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int) = when (val item = items[position]) {
        DataItem.Error -> -1
        DataItem.Loading -> -2
        DataItem.ThumbnailHint -> -3
        DataItem.Margin -> position
        is DataItem.File -> item.id
        is DataItem.NoFiles -> item.folderName?.hashCode() ?: -5
        is DataItem.Title -> item.title?.hashCode() ?: -4
    }.toLong()

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
            val file = items[position] as DataItem.File
            holder.binding.textViewTitle.text = file.name
            holder.binding.textViewDetail.text = file.detail
            holder.binding.textViewDetail.isVisible = !file.detail.isNullOrBlank()
            holder.binding.resultIndicator.setImageResource(file.resultIcon ?: 0)
            holder.binding.imageViewArrow.isVisible = file.file is FileObject.Folder
            holder.binding.progress.isVisible = file.file == null
            val imagePadding = context.resources.getDimensionPixelSize(if (holder.binding.progress.isVisible) R.dimen.margin_1_2 else R.dimen.margin_1)
            holder.binding.imageViewFileIcon.setPadding(imagePadding, imagePadding, imagePadding, imagePadding)

            // Icon
            picasso?.cancelRequest(holder.binding.imageViewFileIcon)
            when {
                file.iconUrl == null || picasso == null -> holder.binding.imageViewFileIcon.setImageDrawable(file.iconPlaceholder)
                else -> picasso?.load(file.iconUrl)?.error(file.iconPlaceholder)?.into(holder.binding.imageViewFileIcon)
            }

            // Click handling
            file.file?.let {
                holder.binding.root.setOnClickListener { _ ->
                    onFileSelected(it)
                }
                holder.itemView.setOnLongClickListener { _ ->
                    onFileMenuOpened(it)
                    true
                }
            } ?: let {
                holder.binding.root.setOnClickListener(null)
                holder.binding.root.setOnLongClickListener(null)
            }
        }

        is ViewHolder.TitleViewHolder -> {
            holder.binding.textViewTitle.text = (items[position] as DataItem.Title).title
                ?: "Your files**"
            holder.binding.buttonAdd.setOnClickListener { onAddItemClicked() }
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

            holder.binding.buttonAdd.setOnClickListener { onAddItemClicked() }
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
        data class File(
            val id: Int,
            val file: FileObject?,
            val name: String,
            val detail: String?,
            val iconUrl: String?,
            val iconPlaceholder: Drawable,
            val resultIcon: Int?,
        ) : DataItem()

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