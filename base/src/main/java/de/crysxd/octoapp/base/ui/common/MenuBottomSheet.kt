package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlinx.android.synthetic.main.fragment_menu_bottom_sheet.*
import kotlinx.android.synthetic.main.item_menu.view.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber


abstract class MenuBottomSheet : BottomSheetDialogFragment() {

    private lateinit var adapter: MenuAdapter
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
        requireOctoActivity().showDialog(throwable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_menu_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.window_background))
        adapter = MenuAdapter(requireContext(), getMenuRes()) {
            lifecycleScope.launch(exceptionHandler) {
                dismiss()
                if (!onMenuItemSelected(it)) {
                    onMenuItemSelectedBase(it)
                }
            }
        }
        menuRecycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            BillingManager.billingFlow().collectLatest {
                setMenuItemVisibility(R.id.menuSupportOctoApp, it.isBillingAvailable && !it.isPremiumActive)
            }
        }
    }

    @MenuRes
    abstract fun getMenuRes(): Int

    fun setMenuItemVisibility(@IdRes id: Int, visible: Boolean) {
        adapter.setMenuItemVisibility(id, visible)
    }

    fun setTitle(title: CharSequence) {
        menuTitle.text = title
        menuTitle.isVisible = true
    }

    abstract suspend fun onMenuItemSelected(@IdRes id: Int): Boolean

    private suspend fun onMenuItemSelectedBase(@IdRes id: Int) = when (id) {
        R.id.menuOpenOctoprint -> Injector.get().openOctoPrintWebUseCase().execute(requireContext())
        R.id.menuGiveFeedback -> SendFeedbackDialog().show(requireActivity().supportFragmentManager, "send-feedback")
        R.id.menuSupportOctoApp -> {
            OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "more_menu"))
            findNavController().navigate(R.id.action_show_purchase_flow)
        }
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
            if (!requireContext().resources.getBoolean(R.bool.night_mode)) {
                decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }

            val padding = requireContext().resources.getDimensionPixelSize(R.dimen.margin_2)
            requireView().setPadding(
                0,
                padding,
                0,
                padding + (activity?.window?.decorView?.rootWindowInsets?.systemWindowInsetBottom ?: 0)
            )

        }
    }

    fun show(fm: FragmentManager) {
        show(fm, "overflow-menu")
    }

    private class MenuAdapter(
        private val context: Context,
        @MenuRes private val menuRes: Int,
        private val callback: (Int) -> Any
    ) : RecyclerView.Adapter<MenuItemViewHolder>() {

        private val menuItems = mutableListOf<MenuItem>()
        private val hiddenItems = mutableListOf<Int>()

        init {
            inflateMenu()
        }

        fun setMenuItemVisibility(@IdRes id: Int, visible: Boolean) {
            if (visible) {
                hiddenItems.remove(id)
            } else {
                hiddenItems.add(id)
            }

            inflateMenu()
        }

        fun inflateMenu() {
            val menu = PopupMenu(context, null).menu
            MenuInflater(context).inflate(menuRes, menu)
            menuItems.clear()
            menuItems.addAll((0.until(menu.size())).map { i ->
                menu[i]
            }.filter {
                !hiddenItems.contains(it.itemId) && it.isVisible
            })
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MenuItemViewHolder(parent)

        override fun getItemCount() = menuItems.size

        override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
            val item = menuItems[position]
            holder.itemView.imageViewIcon.setImageDrawable(item.icon)
            holder.itemView.textViewTitle.text = if (item.itemId == R.id.menuSupportOctoApp) {
                Firebase.remoteConfig.getString("purchase_screen_launch_cta")
            } else {
                item.title
            }
            holder.itemView.setOnClickListener { callback(item.itemId) }
        }
    }

    private class MenuItemViewHolder(parent: ViewGroup) : AutoBindViewHolder(parent, R.layout.item_menu)

}