package de.crysxd.octoapp.base.models

import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import kotlin.reflect.KClass

typealias WidgetClass = KClass<out RecyclableOctoWidget<*, *>>