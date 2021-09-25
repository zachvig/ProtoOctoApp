package de.crysxd.baseui

import androidx.lifecycle.ViewModel
import de.crysxd.octoapp.base.di.ViewModelFactory
import javax.inject.Provider

class BaseViewModelFactory(
    creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelFactory(creators)