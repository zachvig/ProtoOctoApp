package de.crysxd.octoapp.base.ui

import androidx.lifecycle.ViewModel
import javax.inject.Provider

class BaseViewModelFactory(
    creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelFactory(creators)