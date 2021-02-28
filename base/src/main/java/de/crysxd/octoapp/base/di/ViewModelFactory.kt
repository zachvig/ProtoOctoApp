package de.crysxd.octoapp.base.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Provider

open class ViewModelFactory(
        private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val creator = creators[modelClass] ?: creators.entries.firstOrNull {
            modelClass.isAssignableFrom(it.key)
        }?.value ?: throw IllegalArgumentException("Unknown model class $modelClass. Did you define your provides-functions as following? Make sure to use **ViewModel** as return type!\n\n@Provides\n@IntoMap\n@ViewModelKey(${modelClass.simpleName}::class)\nfun provide${modelClass.simpleName}(): ViewModel = ....\n\n ")
        @Suppress("UNCHECKED_CAST")
        return creator.get() as T
    }
}