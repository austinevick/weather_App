package com.example.sampleapp.models

import java.io.Serializable


data class WeatherModel(
    val coordinate: Coordinate,
    val weather: List<Weather>,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val base: String,
    val clouds: Clouds,
    val date: Long,
    val sys: Sys,
    val name: String,
) : Serializable

data class Coordinate(
    val longitude: Double,
    val latitude: Double
) : Serializable

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
) : Serializable

data class Main(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int,
    val sea_level: Int
) : Serializable

data class Wind(
    val speed: Double,
    val degree: Int,
    val gust: Double
) : Serializable

data class Clouds(
    val all: Int
) : Serializable

data class Sys(
    val country: String,
    val sunrise: Long,
    val sunset: Long
) : Serializable