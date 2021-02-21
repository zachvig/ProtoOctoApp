package de.crysxd.octoapp.octoprint.plugins.materialmanager.spoolmanager

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface SpoolManagerApi {

    @GET("plugin/SpoolManager/loadSpoolsByQuery")
    suspend fun listSpools(
        @Query("from") from: Int = 0,
        @Query("to") to: Int = 100,
        @Query("sortColumn") sortColumn: String = "displayName",
        @Query("sortOrder") sortOrder: String = "desc",
        @Query("filterName") filterName: String = "all",
    ): ListSpoolResponse

    @PUT("plugin/SpoolManager/selectSpool")
    suspend fun selectSpool(
        @Body spool: SelectSpoolBody
    )
}