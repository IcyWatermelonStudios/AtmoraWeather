package com.icywatermelonstudios.atmoraweather.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// forecast endpoint, the base api url is https://api.open-meteo.com/
interface ForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("hourly") hourly: String,
        @Query("daily") daily: String,
        // "auto" timezone makes the api return timestamps in location's own timezone
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7,
        // either only celsius or fahrenheit
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        // only kmh, ms, mph, or kn
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
        // only mm or inch
        @Query("precipitation_unit") precipitationUnit: String = "mm",
    ): ForecastResponseDto
}

// geocoding endpoint, the base api url is https://geocoding-api.open-meteo.com/
interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 15,
        // keeps place names and region names in english, matching the app ui currently
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponseDto
}

// variables for api
object ForecastFields {
    const val CURRENT = "temperature_2m,apparent_temperature,relative_humidity_2m,is_day," +
            "precipitation,weather_code,cloud_cover,pressure_msl," +
            "wind_speed_10m,wind_direction_10m,wind_gusts_10m"

    const val HOURLY = "temperature_2m,apparent_temperature,precipitation_probability," +
            "weather_code,wind_speed_10m"

    const val DAILY = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset," +
            "uv_index_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max"
}

// makes the retrofit clients once, app container will hold one instance of this
class ApiFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val forecastApi: ForecastApi by lazy {
        retrofit("https://api.open-meteo.com/").create(ForecastApi::class.java)
    }

    val geocodingApi: GeocodingApi by lazy {
        retrofit("https://geocoding-api.open-meteo.com/").create(GeocodingApi::class.java)
    }
}