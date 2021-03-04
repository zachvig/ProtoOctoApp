package de.crysxd.octoapp.base.ui.common.gcodeshortcut

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.gcodeshortcuts.GcodeShortcutsMenu

class GcodeShortcutLayoutManager(
    private val layout: ViewGroup,
    private val childFragmentManager: FragmentManager,
    private val scroller: HorizontalScrollView? = null,
    private val onClicked: (GcodeHistoryItem) -> Unit,
    private val onInsert: ((GcodeHistoryItem) -> Unit)? = null
) {

    private val otherViewTag = "other_view"
    private var initialLayout = true

    init {
        layout.children.forEach { it.tag = otherViewTag }
    }

    fun showGcodes(gcodes: List<GcodeHistoryItem>) {
        // Remove all old views except the predefined buttons (those have tag == true)
        val removedViews = mutableListOf<Button>()
        layout.children.toList().forEach {
            if (it.tag != otherViewTag) {
                layout.removeView(it)
                removedViews.add(it as Button)
            }
        }

        // Add new views
        gcodes.forEach { gcode ->
            val button = removedViews.firstOrNull { it.tag == gcode.command && it.parent == null }
                ?: LayoutInflater.from(layout.context).inflate(R.layout.widget_gcode_button, layout, false) as Button

            button.text = gcode.name
            button.tag = gcode.command
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (gcode.isFavorite) {
                    R.drawable.ic_round_push_pin_16
                } else {
                    0
                }, 0, 0, 0
            )
            layout.addView(button, 0)
            button.setOnClickListener {
                onClicked(gcode)
            }
            button.setOnLongClickListener {
                MenuBottomSheetFragment.createForMenu(GcodeShortcutsMenu(gcode, onInsert)).show(childFragmentManager)
                true
            }
        }

        // Scroll to end of list the first time we populate the buttons
        if (initialLayout) {
            initialLayout = false
            scroller?.post {
                scroller.scrollTo(layout.width, 0)
            }
        }
    }
}