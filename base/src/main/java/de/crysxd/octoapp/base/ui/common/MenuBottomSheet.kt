package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.core.view.get
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import kotlinx.android.synthetic.main.item_menu.view.*
import kotlinx.coroutines.launch


abstract class MenuBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = RecyclerView(requireContext())
        view.layoutManager = LinearLayoutManager(requireContext())
        view.adapter = MenuAdapter(requireContext(), getMenuRes()) {
            lifecycleScope.launch {
                dismiss()
                if (!onMenuItemSelected(it)) {
                    onMenuItemSelectedBase(it)
                }
            }
        }
        return view
    }

    @MenuRes
    abstract fun getMenuRes(): Int

    abstract suspend fun onMenuItemSelected(@IdRes id: Int): Boolean

    private suspend fun onMenuItemSelectedBase(@IdRes id: Int) = when (id) {
        R.id.menuOpenOctoprint -> Injector.get().octoPrintProvider().octoPrint.value?.let {
            Injector.get().openOctoPrintWebUseCase().execute(Pair(it, requireContext()))
        }
        R.id.menuGiveFeedback -> Injector.get().openEmailClientForFeedbackUseCase().execute(requireContext())
        else -> Unit
    }

    override fun onStart() {
        super.onStart()

        // Fixes dialog hides nav bar on Android O
        if (dialog != null && dialog!!.window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val window = dialog!!.window
            window!!.findViewById<View>(com.google.android.material.R.id.container).fitsSystemWindows = false
            // dark navigation bar icons
            val decorView = window.decorView
            decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

            val padding = requireContext().resources.getDimensionPixelSize(R.dimen.margin_2)
            requireView().setPadding(
                0,
                padding,
                0,
                padding + requireActivity().window.decorView.rootWindowInsets.systemWindowInsetBottom
            )

        }
    }

    fun show(fm: FragmentManager) {
        show(fm, "overflow-menu")
    }

    private class MenuAdapter(context: Context, @MenuRes menuRes: Int, val callback: (Int) -> Any) : RecyclerView.Adapter<MenuItemViewHolder>() {

        private val menuItems: List<MenuItem>

        init {
            val menu = PopupMenu(context, null).menu
            MenuInflater(context).inflate(menuRes, menu)
            menuItems = (0.until(menu.size())).map { i ->
                menu[i]
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MenuItemViewHolder(parent)

        override fun getItemCount() = menuItems.size

        override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
            val item = menuItems[position]
            holder.itemView.imageViewIcon.setImageDrawable(item.icon)
            holder.itemView.textViewTitle.text = item.title
            holder.itemView.setOnClickListener { callback(item.itemId) }
        }
    }

    private class MenuItemViewHolder(parent: ViewGroup) : AutoBindViewHolder(parent, R.layout.item_menu)

}