package de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface FilamentManagerApi {

    @GET("plugin/filamentmanager/spools")
    suspend fun listSpools(): ListSpoolResponse

    @GET("plugin/filamentmanager/selections")
    suspend fun getSelections(): SelectionsResponse

    @PATCH("plugin/filamentmanager/selections/0")
    suspend fun selectSpool(
        @Body spool: SelectSpoolBody
    )
}