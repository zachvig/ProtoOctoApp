package de.crysxd.octoapp.filemanager.ui.file_details

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat

class ReversedDrawingOrderLinearLayoutCompat @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayoutCompat(context, attrs) {

    init {
        isChildrenDrawingOrderEnabled = true
    }

    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int) = childCount - drawingPosition - 1
}
