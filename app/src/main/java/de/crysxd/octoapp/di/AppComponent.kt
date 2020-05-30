package de.crysxd.octoapp.di

import android.app.Application
import android.content.Context
import dagger.Component

@Component(
    modules = [
        AndroidModule::class
    ]
)
interface AppComponent {

    // AndroidModule
    fun context(): Context
    fun app(): Application

}