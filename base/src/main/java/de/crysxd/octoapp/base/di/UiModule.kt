package de.crysxd.octoapp.base.di

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.ui.widget.OctoWidgetRecycler

@Module
class UiModule {

    @Provides
    @BaseScope
    fun provideOctoWidgetRecycler() = OctoWidgetRecycler()
}