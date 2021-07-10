package de.crysxd.octoapp.help.faq

import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.squareup.picasso.Picasso
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ext.suspendedAwait
import de.crysxd.octoapp.base.ui.base.InsetAwareScreen
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.utils.ThemePlugin
import de.crysxd.octoapp.help.R
import de.crysxd.octoapp.help.databinding.HelpDetailFragmentBinding
import io.noties.markwon.Markwon
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import timber.log.Timber
import java.util.concurrent.CancellationException

class HelpDetailFragment : Fragment(), InsetAwareScreen {

    private val args by navArgs<HelpDetailFragmentArgs>()
    private lateinit var binding: HelpDetailFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        HelpDetailFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.faqId == null && args.bug == null) {
            findNavController().popBackStack()
            return
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val markwon = Markwon.builder(view.context)
                .usePlugin(ImagesPlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(ThemePlugin(view.context))
                .build()


            Timber.i("Showing details for: ${args.faqId} or ${args.bug?.title}")
            var bound = false
            args.faqId?.let {
                getFaq(it)?.let { faq ->
                    bindFaq(faq, markwon)
                    bound = true
                }
            }
            args.bug?.let {
                bindBug(it, markwon)
                bound = true
            }

            if (bound) {
                TransitionManager.beginDelayedTransition(binding.root)
                binding.progress.isVisible = false
                binding.scrollView.isVisible = true
            }
        }
    }

    private suspend fun getFaq(faqId: String) = try {
        Firebase.remoteConfig.fetchAndActivate().suspendedAwait()
        val faqs = parseFaqsFromJson(Firebase.remoteConfig.getString("faq"))
        faqs.first { it.id == faqId }
    } catch (e: CancellationException) {
        // Nothing to do
        null
    } catch (e: Exception) {
        Timber.e(e)
        requireOctoActivity().showDialog(getString(R.string.help___content_not_available),
            positiveAction = {
                if (isAdded) {
                    findNavController().popBackStack()
                }
            }
        )
        null
    }

    private fun bindBug(bug: KnownBug, markwon: Markwon) {
        binding.title.text = bug.title
        binding.status.text = getString(R.string.help___status_x, bug.status)
        markwon.setMarkdown(binding.content, bug.content ?: "")
        binding.videoThumbnailContainer.isVisible = false
    }

    private fun bindFaq(faq: Faq, markwon: Markwon) {
        binding.title.text = faq.title
        binding.status.isVisible = false
        markwon.setMarkdown(binding.content, faq.content ?: "")
        if (!faq.youtubeThumbnailUrl.isNullOrBlank() && !faq.youtubeUrl.isNullOrBlank()) {
            Picasso.get().load(faq.youtubeThumbnailUrl).into(binding.videoThumbnail)
            binding.videoThumbnailContainer.setOnClickListener {
                Uri.parse(faq.youtubeUrl).open(requireOctoActivity())
            }
        } else {
            binding.videoThumbnailContainer.isVisible = false
        }
    }

    override fun handleInsets(insets: Rect) {
        binding.statusBarScrim.updateLayoutParams {
            height = insets.top
        }

        if (!binding.videoThumbnailContainer.isVisible) {
            binding.root.updatePadding(top = insets.top)
        }

        binding.root.updatePadding(bottom = insets.bottom)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = false
    }
}