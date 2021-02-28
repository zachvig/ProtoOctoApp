package de.crysxd.octoapp.base.ui.common.help

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.FaqFragmentBinding
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.base.InsetAwareScreen
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

class FaqFragment : Fragment(), InsetAwareScreen {

    private val args by navArgs<FaqFragmentArgs>()
    private lateinit var binding: FaqFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FaqFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val faq = args.faq
        binding.title.text = faq.title
        Markwon.builder(view.context)
            .usePlugin(ImagesPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(ThemePlugin(view.context))
            .build()
            .setMarkdown(binding.content, faq.content ?: "")

        if (faq.youtubeThumbnailUrl != null && faq.youtubeUrl != null) {
            Picasso.get().load(faq.youtubeThumbnailUrl).into(binding.videoThumbnail)
            binding.videoThumbnailContainer.setOnClickListener {
                Uri.parse(faq.youtubeUrl).open(it.context)
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
}