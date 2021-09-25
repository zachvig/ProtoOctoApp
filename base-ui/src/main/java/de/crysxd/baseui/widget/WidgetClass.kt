package de.crysxd.octoapp.base.data.models

import de.crysxd.baseui.widget.RecyclableOctoWidget
import kotlin.reflect.KClass

typealias WidgetClass = KClass<out RecyclableOctoWidget<*, *>>