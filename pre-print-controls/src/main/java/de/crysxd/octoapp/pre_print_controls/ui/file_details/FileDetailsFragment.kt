package de.crysxd.octoapp.pre_print_controls.ui.file_details

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.Injector
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_file_details.*
import kotlinx.coroutines.runBlocking
import java.text.DateFormat
import java.util.*
import de.crysxd.octoapp.base.di.Injector as BaseInjector

class FileDetailsFragment : Fragment(R.layout.fragment_file_details) {

    private val viewModel: FileDetailsViewModel by injectViewModel(Injector.get().viewModelFactory())
    private val file by lazy { navArgs<FileDetailsFragmentArgs>().value.file }
    private val picasso by lazy { Injector.get().picasso() }
    private val adapter by lazy { Adapter(this, file) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textViewFileName.text = file.display

        buttonStartPrint.setOnClickListener { viewModel.startPrint(file) }

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = adapter.itemCount
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = when (position) {
                adapter.positionPrintInfo -> "Print info"
                adapter.positionFileInfo -> "File"
                adapter.positionHistory -> "History"
                else -> ""
            }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = animateViewPagerHeight(calculateHeightForPage(position))
        })

        picasso.observe(viewLifecycleOwner, {
            file.thumbnail?.let { url ->
                it.load(url).into(imageViewPreview)
            }
        })
    }

    private fun animateViewPagerHeight(height: Int) {
        val transition = AutoTransition()
        transition.excludeChildren(viewPager, true)
        TransitionManager.beginDelayedTransition(view as ViewGroup, transition)
        viewPager.layoutParams = viewPager.layoutParams.also {
            it.height = height
        }
    }

    private fun calculateHeightForPage(position: Int): Int {
        val view = adapter.createFragment(position).requireView()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(viewPager.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        return view.measuredHeight
    }

    class Adapter(private val fragment: Fragment, private val file: FileObject.File) : FragmentStateAdapter(fragment) {
        val positionPrintInfo = 0
        val positionFileInfo = 1
        val positionHistory = 2
        private val formatDurationUseCase = BaseInjector.get().formatDurationUseCase()
        private val fragments = (0 until itemCount).map { doCreateFragment(it) }

        override fun getItemCount() = 3

        override fun createFragment(position: Int) = fragments[position]

        private fun doCreateFragment(position: Int) = runBlocking {
            val details = when (position) {
                positionPrintInfo -> listOf(
                    DetailListFragment.Detail(
                        icon = R.drawable.ic_round_access_time_24,
                        title = R.string.print_time,
                        value = file.gcodeAnalysis?.estimatedPrintTime?.let { formatDurationUseCase.execute(it) }
                    ),
                    DetailListFragment.Detail(
                        icon = R.drawable.ic_round_straighten_24,
                        title = R.string.model_size,
                        value = file.gcodeAnalysis?.dimensions?.let { String.format(Locale.getDefault(), "%.1f × %.1f × %.1f mm", it.width, it.depth, it.height) }
                    ),
                    DetailListFragment.Detail(
                        icon = R.drawable.ic_round_reorder_24,
                        title = R.string.filament_use,
                        value = file.gcodeAnalysis?.filament?.let {
                            val totalLength = listOfNotNull(it.tool0, it.tool1).sumByDouble { s -> s.length }
                            val totalVolume = listOfNotNull(it.tool0, it.tool1).sumByDouble { s -> s.volume }
                            String.format(Locale.getDefault(), "%.02f m / %.02f cm³", totalLength / 1000, totalVolume)
                        }
                    ),
                )

                positionFileInfo -> listOf(
                    DetailListFragment.Detail(
                        icon = when (file.origin) {
                            FileObject.FILE_ORIGIN_SD -> R.drawable.ic_round_sd_storage_24
                            else -> R.drawable.ic_round_storage_24
                        },
                        title = R.string.location,
                        value = when (file.origin) {
                            FileObject.FILE_ORIGIN_SD -> fragment.getString(R.string.file_location_sd_card)
                            FileObject.FILE_ORIGIN_LOCAL -> fragment.getString(R.string.file_location_local)
                            else -> fragment.getString(R.string.file_location_unknown)
                        },
                    ),
                    DetailListFragment.Detail(
                        icon = R.drawable.ic_round_folder_open_24,
                        title = R.string.path,
                        value = "/" + file.path.removeSuffix(file.name).removeSuffix("/")
                    ),
                    DetailListFragment.Detail(
                        icon = R.drawable.ic_round_square_foot_24,
                        title = R.string.size,
                        value = file.size.asStyleFileSize()
                    ),
                    DetailListFragment.Detail(
                        icon = R.drawable.ic_round_calendar_today_24,
                        title = R.string.uploaded,
                        value = formatDate(file.date)
                    )
                )

                positionHistory -> listOf(
                    DetailListFragment.Detail(
                        icon = R.drawable.ic_round_history_24,
                        title = R.string.last_print,
                        value = file.prints?.last?.let {
                            fragment.getString(
                                if (it.success) {
                                    R.string.last_print_at_x_success
                                } else {
                                    R.string.last_print_at_x_failure
                                }, formatDate(it.date)
                            )
                        }
                    ),
                    DetailListFragment.Detail(
                        icon = R.drawable.ic_round_check_circle_24,
                        title = R.string.completed,
                        value = file.prints?.success?.let { fragment.getString(R.string.x_times, it) }
                    ),
                    DetailListFragment.Detail(
                        icon = R.drawable.ic_round_cancel_24,
                        title = R.string.failures,
                        value = file.prints?.failure?.let { fragment.getString(R.string.x_times, it) }
                    ),
                )

                else -> emptyList()

            }.filter { !it.value.isNullOrBlank() }

            return@runBlocking DetailListFragment.createFor(details)
        }

        private fun formatDate(time: Long) = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time * 1000))

    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        requireOctoActivity().octo.isVisible = true
    }
}