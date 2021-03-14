package de.crysxd.octoapp.base.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.ArrayList

@Parcelize
class WidgetList : ArrayList<WidgetClass>(), Parcelable