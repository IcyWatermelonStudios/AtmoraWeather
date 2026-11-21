package com.icywatermelonstudios.atmoraweather.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

// locations

// saved place, serializable because it's stored locally in a json
@Serializable
data class Location(
    val id: Long,
    val name: String,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
) {
    val subtitle: String
        get() = listOfNotNull(region, country).joinToString(", ")
}

// units

enum class TemperatureUnit(val apiValue: String, val symbol: String) {
    CELSIUS("celsius", "°C"),
    FAHRENHEIT("fahrenheit", "°F"),
}

enum class WindSpeedUnit(val apiValue: String, val label: String) {
    KMH("kmh", "km/h"),
    MS("ms", "m/s"),
    MPH("mph", "mph"),
    KNOTS("kn", "kn"),
}

enum class PrecipitationUnit(val apiValue: String, val label: String) {
    MM("mm", "mm"),
    INCH("inch", "in"),
}

@Serializable
data class UnitSettings(
    val temperature: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeed: WindSpeedUnit = WindSpeedUnit.KMH,
    val precipitation: PrecipitationUnit = PrecipitationUnit.MM,
)

// weather data itself

// conditions right now
data class CurrentWeather(
    val time: LocalDateTime,
    val temperature: Double,
    val apparentTemperature: Double,
    val humidity: Int,
    val isDay: Boolean,
    val precipitation: Double,
    // code converted by Codes.kt
    val weatherCode: Int,
    val cloudCover: Int,
    val pressure: Double,
    val windSpeed: Double,
    // degrees, 0 is wind from the north
    val windDirection: Int,
    val windGusts: Double,
) {
    // compass label for direction, for example 90 -> "E"
    val windDirectionLabel: String
        get() {
            val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = (((windDirection % 360) + 360) % 360 / 45.0 + 0.5).toInt() % 8
            return points[index]
        }
}

// one row for hourly weather
data class HourlyForecast(
    val time: LocalDateTime,
    val temperature: Double,
    val apparentTemperature: Double,
    // this is percent, null when api doesn't have a number for the hour
    val precipitationProbability: Int?,
    val weatherCode: Int,
    val windSpeed: Double,
)

// one row for daily weather
data class DailyForecast(
    val date: LocalDate,
    val weatherCode: Int,
    val temperatureMax: Double,
    val temperatureMin: Double,
    val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?,
    val uvIndexMax: Double?,
    val precipitationSum: Double,
    val precipitationProbabilityMax: Int?,
    val windSpeedMax: Double,
)

// everything the ui needs for a location
data class WeatherReport(
    val location: Location,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val units: UnitSettings,
    // when data was downloaded, shows in Updated at and debug settings
    val fetchedAt: LocalDateTime,
)