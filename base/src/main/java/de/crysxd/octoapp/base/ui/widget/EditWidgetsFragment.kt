package de.crysxd.octoapp.base.ui.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.databinding.EditWidgetsFragmentBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.WidgetPreferences
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity

class EditWidgetsFragment : BaseWidgetHostFragment() {

    override val viewModel by injectViewModel<EditWidgetsViewModel>()
    private val destinationId by lazy { navArgs<EditWidgetsFragmentArgs>().value.destinationId }
    private val widgets by lazy { navArgs<EditWidgetsFragmentArgs>().value.widgets }
    private lateinit var binding: EditWidgetsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        EditWidgetsFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.widgetList.isEditMode = true
        binding.widgetList.connectToLifecycle(viewLifecycleOwner)
        binding.confirm.setOnClickListener {
            val prefs = WidgetPreferences(destinationId, binding.widgetList.widgets)
            Injector.get().widgetPreferencesRepository().setWidgetOrder(destinationId, prefs)
            findNavController().popBackStack()
        }

        val prefs = Injector.get().widgetPreferencesRepository().getWidgetOrder(destinationId) ?: WidgetPreferences(destinationId, emptyList())
        val sortedWidgets = prefs.prepare(widgets)
        binding.widgetList.showWidgets(this, sortedWidgets)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = false
        binding.widgetList.setupWithToolbar(requireOctoActivity())
    }

    override fun requestTransition() = Unit

    override fun reloadWidgets() = Unit

}