package de.crysxd.octoapp.widgets

import android.view.View
import android.widget.RemoteViews
import androidx.annotation.IdRes

fun RemoteViews.setViewVisibility(@IdRes viewId: Int, visible: Boolean) = setViewVisibility(viewId, if (visible) View.VISIBLE else View.GONE)