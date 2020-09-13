package de.crysxd.octoapp.pre_print_controls.ui.file_details

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_details_list.*

class DetailListFragment : Fragment(R.layout.fragment_details_list) {

    companion object {
        private const val KEY_DETAILS = "details"

        fun createFor(details: List<Detail>) = DetailListFragment().also {
            val list = ArrayList<Detail>().apply { addAll(details) }
            it.arguments = Bundle().apply { putParcelableArrayList(KEY_DETAILS, list) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val details = arguments?.getParcelableArrayList<Detail>(KEY_DETAILS)
        bind(details?.getOrNull(0), imageViewIcon1, textViewTitle1, textViewValue1)
        bind(details?.getOrNull(1), imageViewIcon2, textViewTitle2, textViewValue2)
        bind(details?.getOrNull(2), imageViewIcon3, textViewTitle3, textViewValue3)
        bind(details?.getOrNull(3), imageViewIcon4, textViewTitle4, textViewValue4)
    }

    private fun bind(detail: Detail?, iconView: ImageView, titleView: TextView, valueView: TextView) {
        iconView.isVisible = detail != null
        titleView.isVisible = iconView.isVisible
        valueView.isVisible = iconView.isVisible

        detail?.let {
            iconView.setImageResource(it.icon)
            titleView.text = getString(it.title)
            valueView.text = it.value
        }
    }

    @Parcelize
    data class Detail(
        @StringRes val title: Int,
        val value: CharSequence?,
        @DrawableRes val icon: Int
    ) : Parcelable
}