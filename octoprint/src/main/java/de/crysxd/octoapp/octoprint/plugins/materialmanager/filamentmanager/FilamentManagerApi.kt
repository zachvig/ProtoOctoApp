package de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface FilamentManagerApi {

    @GET("plugin/filamentmanager/spools")
    suspend fun listSpools(): ListSpoolResponse

    @GET("plugin/filamentmanager/selections")
    suspend fun getSelections(): SelectionsResponse

    @PUT("plugin/SpoolManager/selectSpool")
    suspend fun selectSpool(
        @Body spool: SelectSpoolBody
    )
}