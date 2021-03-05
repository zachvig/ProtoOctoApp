package de.crysxd.octoapp.help

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
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
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ext.suspendedAwait
import de.crysxd.octoapp.base.ui.base.InsetAwareScreen
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.databinding.HelpDetailFragmentBinding
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import timber.log.Timber

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
    } catch (e: Exception) {
        Timber.e(e)
        requireOctoActivity().showDialog("This content is currently not available. Try again later!", positiveAction = { findNavController().popBackStack() })
        null
    }

    private fun bindBug(bug: KnownBug, markwon: Markwon) {
        binding.title.text = bug.title
        binding.status.text = "Status: ${bug.status}"
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

    private class ThemePlugin(private val context: Context) : AbstractMarkwonPlugin() {

        override fun configureTheme(builder: MarkwonTheme.Builder) {
            super.configureTheme(builder)
            val res = context.resources
            val attrs = arrayOf(R.attr.fontFamily).toIntArray()
            val typeface = context.obtainStyledAttributes(
                R.style.OctoTheme_TextAppearance_Title,
                attrs
            ).use {
                ResourcesCompat.getFont(context, it.getResourceId(0, 0))
            } ?: Typeface.DEFAULT

            builder.linkColor(ContextCompat.getColor(context, R.color.accent))
            builder.headingBreakHeight(0)
            builder.headingTextSizeMultipliers(
                arrayOf(
                    1.714f, // H1
                    1.571f, // H2
                    1.429f, // H3
                    1.286f, // H4
                    1.143f, // H5
                    1f, // H6
                ).toFloatArray()
            )
            builder.headingTypeface(typeface)
            builder.bulletWidth(res.getDimension(R.dimen.margin_0_1).toInt())
            builder.bulletListItemStrokeWidth(res.getDimension(R.dimen.margin_2).toInt())
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = false
    }
}