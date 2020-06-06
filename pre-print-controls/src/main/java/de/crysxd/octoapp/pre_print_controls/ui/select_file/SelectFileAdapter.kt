package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.android.synthetic.main.list_item_file.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

class SelectFileAdapter(private val callback: (FileObject) -> Unit) : RecyclerView.Adapter<FileViewHolder>() {

    var files: List<FileObject> = emptyList()
        set(value) {
            // Sort files by date, folders by name
            val groups = value.groupBy { it::class.java }
            val files = groups[FileObject.File::class.java]?.sortedByDescending { (it as FileObject.File).date } ?: emptyList()
            val folders = groups[FileObject.Folder::class.java]?.sortedBy { it.display } ?: emptyList()

            field = listOf(folders, files).flatten()
            notifyDataSetChanged()
        }

    private val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    override fun getItemCount() = files.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FileViewHolder(parent)

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
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
                holder.imageViewFileIcon.setImageResource(
                    if (file.typePath.contains(FileObject.FILE_TYPE_MACHINE_CODE)) {
                        R.drawable.ic_outline_print_24
                    } else {
                        R.drawable.ic_outline_insert_drive_file_24
                    }
                )
            }
        }.let {}

        holder.itemView.setOnClickListener {
            callback(file)
        }
    }

    private fun styleFileSize(size: Long): String {
        val kb = size / 8
        val mb = kb / 1000f
        val gb = mb / 1000f

        return when {
            gb > 1 -> DecimalFormat("#.## 'GB'").format(gb)
            mb > 1 -> DecimalFormat("#.# 'MB'").format(mb)
            else -> DecimalFormat("# 'kB'").format(mb)
        }
    }
}