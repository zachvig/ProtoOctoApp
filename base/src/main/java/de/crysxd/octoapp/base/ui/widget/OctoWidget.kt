package de.crysxd.octoapp.base.ui.widget

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.BaseViewModel
import kotlinx.android.extensions.LayoutContainer

abstract class OctoWidget(val parent: Fragment) : LayoutContainer {

    protected lateinit var view: View

    override val containerView: View
        get() = view

    protected val viewLifecycleOwner = parent.viewLifecycleOwner

    abstract fun getTitle(context: Context): String

    abstract fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View

    abstract fun onViewCreated(view: View)

    fun getView(context: Context, container: ViewGroup): View {
        return if (!::view.isInitialized) {
            view = onCreateView(LayoutInflater.from(context), container)
            onViewCreated(view)
            view
        } else {
            view
        }
    }

    fun setupBaseViewModel(baseViewModel: BaseViewModel) {
        baseViewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            (parent as? BaseFragment)?.handleError(it)
        })
        baseViewModel.messages.observe(viewLifecycleOwner, Observer {
            (parent as? BaseFragment)?.handleMessage(it)
        })
        baseViewModel.navContoller = parent.findNavController()
    }
}