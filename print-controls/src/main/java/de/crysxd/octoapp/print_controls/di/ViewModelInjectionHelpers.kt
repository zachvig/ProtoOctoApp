package de.crysxd.octoapp.print_controls.di

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.widget.OctoWidget

/**
 * For Actvities, allows declarations like
 * ```
 * val myViewModel: ViewModelClass by injectViewModel()
 * ```
 */
inline fun <reified VM : ViewModel> FragmentActivity.injectViewModel(provider: ViewModelProvider.Factory = Injector.get().viewModelFactory()) = lazy {
    ViewModelProvider(this, provider)[VM::class.java]
}


internal inline fun <reified VM : ViewModel> OctoWidget.injectViewModel(provider: ViewModelProvider.Factory = Injector.get().viewModelFactory()) = lazy {
    val vm = ViewModelProvider(this.parent, provider)[VM::class.java]
    (vm as? BaseViewModel)?.let { this.setupBaseViewModel(it) }
    vm
}

internal inline fun <reified VM : ViewModel> OctoWidget.injectActivityViewModel(provider: ViewModelProvider.Factory = Injector.get().viewModelFactory()) = lazy {
    val vm = ViewModelProvider(this.parent.requireActivity(), provider)[VM::class.java]
    (vm as? BaseViewModel)?.let { this.setupBaseViewModel(it) }
    vm
}

/**
 * For Fragments, allows declarations like
 * ```
 * val myViewModel: ViewModelClass by injectViewModel()
 * ```
 */
inline fun <reified VM : ViewModel> Fragment.injectViewModel(provider: ViewModelProvider.Factory = Injector.get().viewModelFactory()) = lazy {
    ViewModelProvider(this, provider)[VM::class.java]
}

/**
 * Like [Fragment.injectViewModel] for Fragments that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified VM : ViewModel> Fragment.injectActivityViewModel(provider: ViewModelProvider.Factory = Injector.get().viewModelFactory()) = lazy {
    ViewModelProvider(requireActivity(), provider)[VM::class.java]
}

/**
 * Like [Fragment.injectViewModel] for Fragments that want a [ViewModel] scoped to the parent
 * Fragment.
 */
inline fun <reified VM : ViewModel> Fragment.injectParentViewModel(provider: ViewModelProvider.Factory = Injector.get().viewModelFactory()) = lazy {
    ViewModelProvider(requireParentFragment(), provider)[VM::class.java]
}