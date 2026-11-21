package com.icywatermelonstudios.atmoraweather.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// this class is made for responses of the api

// forecast api, used well for forecast itself. api path https://api.open-meteo.com/v1/forecast

@Serializable
data class ForecastResponseDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // the timezone name is empty. i request timezone=auto so it's local
    val timezone: String = "",
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val current: CurrentDto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null,
)

// current weather, used by overview view
@Serializable
data class CurrentDto(
    val time: String = "", // local time in ISO format without the user's offset
    @SerialName("temperature_2m") val temperature: Double = 0.0,
    @SerialName("apparent_temperature") val apparentTemperature: Double = 0.0,
    @SerialName("relative_humidity_2m") val humidity: Int = 0,
    // 1 is day, 0 is night. used for automatic theme depending on the time (fire af feature)
    @SerialName("is_day") val isDay: Int = 1,
    val precipitation: Double = 0.0,
    // weather interpretation code, translated to text for the user in Codes.kt
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("cloud_cover") val cloudCover: Int = 0,
    @SerialName("pressure_msl") val pressure: Double = 0.0,
    @SerialName("wind_speed_10m") val windSpeed: Double = 0.0,
    @SerialName("wind_direction_10m") val windDirection: Int = 0,
    @SerialName("wind_gusts_10m") val windGusts: Double = 0.0,
)

// hourly weather, used by hourly view
// returned as parallel arrays, index i in every list describes the same hour
@Serializable
data class HourlyDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
)

// daily weather, used by daily view
// same thing as hourly
@Serializable
data class DailyDto(
    // local time for example 2026-11-21
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    // local time for example 2026-11-21T07:45
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double?> = emptyList(),
)

// geocoding api, used for determining location when adding it. api path https://geocoding-api.open-meteo.com/v1/search

@Serializable
data class GeocodingResponseDto(
    // the results key is omitted entirely when nothing was found so it must be nullable
    val results: List<GeocodingResultDto>? = null,
)

@Serializable
data class GeocodingResultDto(
    val id: Long = 0,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String? = null,
    // first admin area, very useful to tell if cities are different when having same names
    val admin1: String? = null,
    val timezone: String? = null,
)