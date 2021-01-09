package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.BaseBottomSheetDialogFragment
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.usecase.SetAppLanguageUseCase
import de.crysxd.octoapp.base.usecase.execute
import kotlinx.android.synthetic.main.fragment_menu_bottom_sheet.*
import kotlinx.android.synthetic.main.fragment_menu_bottom_sheet_premium_header.*
import kotlinx.android.synthetic.main.fragment_menu_bottom_sheet_premium_header.view.*
import kotlinx.android.synthetic.main.item_menu.view.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber


abstract class MenuBottomSheet : BaseBottomSheetDialogFragment() {

    private lateinit var adapter: MenuAdapter
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
        requireOctoActivity().showDialog(throwable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_menu_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MenuAdapter(requireContext(), getMenuRes()) {
            lifecycleScope.launch(exceptionHandler) {
                if (!onMenuItemSelected(it)) {
                    onMenuItemSelectedBase(it)
                }
                dismiss()
            }
        }
        menuRecycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {

            val appLanguage = Injector.get().getAppLanguageUseCase().execute(Unit)
            setMenuItemVisibility(R.id.menuChangeLanguage, appLanguage.canSwitchLocale)
            setMenuItemTitle(R.id.menuChangeLanguage, appLanguage.switchLanguageText ?: "")

            BillingManager.billingFlow().collectLatest {
                setMenuItemTitle(R.id.menuSupportOctoApp, Firebase.remoteConfig.getString("purchase_screen_launch_cta"))
                setMenuItemVisibility(R.id.menuSupportOctoApp, !it.isPremiumActive && it.isBillingAvailable)

                val showHeader = adapter.containsMenuItem(R.id.menuSupportOctoApp) && it.isPremiumActive
                premiumHeaderStub?.isVisible = showHeader
                premiumHeader?.isVisible = showHeader
                premiumHeader?.chevron?.isVisible = it.isPremiumFromSubscription
                if (it.isPremiumFromSubscription) {
                    premiumHeader?.setOnClickListener {
                        try {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/account/subscriptions?package=${requireContext().packageName}")
                                )
                            )
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
            }
        }
    }

    @MenuRes
    abstract fun getMenuRes(): Int

    fun setMenuItemVisibility(@IdRes id: Int, visible: Boolean) {
        adapter.setMenuItemVisibility(id, visible)
    }

    fun setMenuItemTitle(@IdRes id: Int, title: CharSequence) {
        adapter.setMenuItemTitle(id, title)
    }

    fun setTitle(title: CharSequence) {
        menuTitle.text = title
        menuTitle.isVisible = true
    }

    abstract suspend fun onMenuItemSelected(@IdRes id: Int): Boolean

    private suspend fun onMenuItemSelectedBase(@IdRes id: Int) {
        when (id) {
            R.id.menuChangeLanguage -> GlobalScope.launch {
                val newLocale = Injector.get().getAppLanguageUseCase().execute(Unit).switchLanguageLocale
                Injector.get().setAppLanguageUseCase().execute(SetAppLanguageUseCase.Param(newLocale, requireActivity()))
            }
            R.id.menuOpenOctoprint -> Injector.get().openOctoPrintWebUseCase().execute(requireContext())
            R.id.menuGiveFeedback -> SendFeedbackDialog().show(requireActivity().supportFragmentManager, "send-feedback")
            R.id.menuSignOut -> Injector.get().signOutUseCase().execute()
            R.id.menuSupportOctoApp -> {
                OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "more_menu"))
                findNavController().navigate(R.id.action_show_purchase_flow)
            }
            else -> Unit
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
        private val overrideTitles = mutableMapOf<Int, CharSequence>()

        init {
            inflateMenu()
        }

        fun containsMenuItem(@IdRes id: Int) = getMenuItems().any { it.itemId == id }

        fun setMenuItemVisibility(@IdRes id: Int, visible: Boolean) {
            if (visible) {
                hiddenItems.remove(id)
            } else {
                hiddenItems.add(id)
            }

            inflateMenu()
        }

        fun setMenuItemTitle(@IdRes id: Int, title: CharSequence) {
            overrideTitles[id] = title
            inflateMenu()
        }

        private fun inflateMenu() {
            val menu = PopupMenu(context, null).menu
            MenuInflater(context).inflate(menuRes, menu)
            menuItems.clear()
            menuItems.addAll(getMenuItems().filter {
                !hiddenItems.contains(it.itemId) && it.isVisible
            })
            notifyDataSetChanged()
        }

        private fun getMenuItems(): List<MenuItem> {
            val menu = PopupMenu(context, null).menu
            MenuInflater(context).inflate(menuRes, menu)
            return (0.until(menu.size())).map { i ->
                menu[i]
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MenuItemViewHolder(parent)

        override fun getItemCount() = menuItems.size

        override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
            val item = menuItems[position]
            holder.itemView.imageViewIcon.setImageDrawable(item.icon)
            holder.itemView.textViewTitle.text = overrideTitles.getOrElse(item.itemId) { item.title }
            holder.itemView.setOnClickListener { callback(item.itemId) }
        }
    }

    private class MenuItemViewHolder(parent: ViewGroup) : AutoBindViewHolder(parent, R.layout.item_menu)

}