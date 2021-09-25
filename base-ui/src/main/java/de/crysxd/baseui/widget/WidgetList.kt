package de.crysxd.baseui.widget

import android.os.Parcelable
import de.crysxd.octoapp.base.data.models.WidgetType
import kotlinx.parcelize.Parcelize
import java.util.ArrayList

@Parcelize
class WidgetList : ArrayList<WidgetType>(), Parcelable