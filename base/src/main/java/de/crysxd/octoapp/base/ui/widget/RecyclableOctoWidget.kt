package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.databinding.WidgetFrameBinding
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity

abstract class RecyclableOctoWidget<T : ViewBinding, R : BaseViewModel>(context: Context) {

    protected lateinit var parent: BaseWidgetHostFragment
        private set
    protected abstract val binding: T
    private val frameBinding = WidgetFrameBinding.inflate(LayoutInflater.from(context))
    val view: View = frameBinding.root
    protected lateinit var baseViewModel: R
        private set
    protected val context: Context get() = binding.root.context

    fun attach(parent: BaseWidgetHostFragment) {
        this.parent = parent

        if (binding.root.parent == null) {
            frameBinding.container.addView(binding.root)
            frameBinding.title.text = getTitle(frameBinding.title.context)
            frameBinding.title.isVisible = frameBinding.title.text.isNotEmpty()
            getActionIcon()?.let {
                frameBinding.action.setImageResource(it)
                frameBinding.action.setOnClickListener {
                    recordInteraction()
                    onAction()
                }
            } ?: let {
                frameBinding.action.isVisible = false
            }
            binding.root.updateLayoutParams {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }

        createNewViewModel(parent)?.let {
            baseViewModel = it
            setUpViewModel(it)
        }
    }

    fun recordInteraction() {
        OctoAnalytics.logEvent(OctoAnalytics.Event.WidgetInteraction(getAnalyticsName()))
    }

    protected fun setUpViewModel(baseViewModel: BaseViewModel) {
        parent.requireOctoActivity().observeErrorEvents(baseViewModel.errorLiveData)
        parent.requireOctoActivity().observerMessageEvents(baseViewModel.messages)
        baseViewModel.navContoller = parent.findNavController()
    }

    abstract fun createNewViewModel(parent: BaseWidgetHostFragment): R?
    open fun onResume(lifecycleOwner: LifecycleOwner) = Unit
    open fun onPause() = Unit
    open fun isVisible(): Boolean = true
    abstract fun getTitle(context: Context): String?
    abstract fun getAnalyticsName(): String

    @DrawableRes
    open fun getActionIcon(): Int? = null
    open fun onAction() = Unit

}