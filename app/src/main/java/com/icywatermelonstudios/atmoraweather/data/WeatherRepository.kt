package com.icywatermelonstudios.atmoraweather.data

import com.icywatermelonstudios.atmoraweather.data.model.Location
import com.icywatermelonstudios.atmoraweather.data.model.UnitSettings
import com.icywatermelonstudios.atmoraweather.data.model.WeatherReport
import com.icywatermelonstudios.atmoraweather.data.remote.ForecastApi
import com.icywatermelonstudios.atmoraweather.data.remote.ForecastFields
import com.icywatermelonstudios.atmoraweather.data.remote.GeocodingApi
import com.icywatermelonstudios.atmoraweather.data.remote.toLocation
import com.icywatermelonstudios.atmoraweather.data.remote.toWeatherReport
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import android.util.Log

// the only class that talks with network
class WeatherRepository(
    private val forecastApi: ForecastApi,
    private val geocodingApi: GeocodingApi,
) {
    // downloads the location
    suspend fun getWeather(location: Location, units: UnitSettings): Result<WeatherReport> =
        suspendRunCatching {
            forecastApi.getForecast(
                latitude = location.latitude,
                longitude = location.longitude,
                current = ForecastFields.CURRENT,
                hourly = ForecastFields.HOURLY,
                daily = ForecastFields.DAILY,
                temperatureUnit = units.temperature.apiValue,
                windSpeedUnit = units.windSpeed.apiValue,
                precipitationUnit = units.precipitation.apiValue,
            ).toWeatherReport(location = location, units = units)
        }

    suspend fun searchCities(query: String): Result<List<Location>> = suspendRunCatching {
        val name = query.trim()
        // The geocoding API needs at least 2 characters to return useful results
        if (name.length < 2) {
            emptyList()
        } else {
            geocodingApi.searchCity(name)
                .results
                .orEmpty()
                .map { it.toLocation() }
        }
    }
}

// this is like runCatching but rethrows cancellationexception
private inline fun <T> suspendRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("WeatherRepo", "Request failed", e)
        Result.failure(e)
    }

// turns any failure into a normal message and not gibberish for user
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> "Server error (${code()}). Please try again later."
    is IOException -> "No internet connection. Check your network and try again."
    else -> "Something went wrong. Please try again."
}