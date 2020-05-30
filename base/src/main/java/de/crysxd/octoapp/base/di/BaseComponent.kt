package de.crysxd.octoapp.base.di

import android.app.Application
import android.content.Context
import dagger.Component
import de.crysxd.octoapp.base.OctoPrintRepository
import okhttp3.logging.HttpLoggingInterceptor

@BaseScope
@Component(
    modules = [
        AndroidModule::class,
        LoggingModule::class,
        OctoPrintModule::class
    ]
)
interface BaseComponent {

    // AndroidModule
    fun context(): Context
    fun app(): Application

    // LoggingModule
    fun httpLoggingInterceptor(): HttpLoggingInterceptor

    // OctoprintModule
    fun octorPrintRepository(): OctoPrintRepository

}