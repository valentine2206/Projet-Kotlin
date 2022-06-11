package fr.epf.vlime.API

import retrofit2.http.GET
import fr.epf.vlime.model.Station

interface StationApi {
    @GET("get-all-stations")
    suspend fun getStations() : List<Station>
}