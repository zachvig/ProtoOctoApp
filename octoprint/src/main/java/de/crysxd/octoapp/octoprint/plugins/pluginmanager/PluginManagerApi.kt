package de.crysxd.octoapp.octoprint.plugins.pluginmanager

import retrofit2.http.GET

interface PluginManagerApi {

    @GET("plugin/pluginmanager/plugins")
    suspend fun getPlugins(): PluginList
}