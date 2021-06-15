package com.example.sampleapp.network

import com.example.sampleapp.models.WeatherModel
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call

interface WeatherService {
    @GET("2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units")units:String?,
        @Query("appid")appid: String?

    ): Call<WeatherModel>
}