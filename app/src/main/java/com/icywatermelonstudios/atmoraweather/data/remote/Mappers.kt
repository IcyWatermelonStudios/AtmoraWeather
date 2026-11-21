package com.icywatermelonstudios.atmoraweather.data.remote

import com.icywatermelonstudios.atmoraweather.data.model.CurrentWeather
import com.icywatermelonstudios.atmoraweather.data.model.DailyForecast
import com.icywatermelonstudios.atmoraweather.data.model.HourlyForecast
import com.icywatermelonstudios.atmoraweather.data.model.Location
import com.icywatermelonstudios.atmoraweather.data.model.UnitSettings
import com.icywatermelonstudios.atmoraweather.data.model.WeatherReport
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// converts a geocoding result into a saveable location
fun GeocodingResultDto.toLocation(): Location = Location(
    id = id,
    name = name,
    region = admin1,
    country = country,
    latitude = latitude,
    longitude = longitude,
)

// converts the whole forecast into a weatherreport class
fun ForecastResponseDto.toWeatherReport(
    location: Location,
    units: UnitSettings,
    fetchedAt: LocalDateTime = LocalDateTime.now(),
): WeatherReport {
    val currentDto = current ?: error("Forecast response has no current weather.")
    val currentWeather = currentDto.toCurrentWeather()
    // the api returns hours starting at 00:00 today, keep only the current hour and later
    val firstHour = currentWeather.time.truncatedTo(ChronoUnit.HOURS)

    return WeatherReport(
        location = location,
        current = currentWeather,
        hourly = hourly?.toHourlyList(firstHour).orEmpty(),
        daily = daily?.toDailyList().orEmpty(),
        units = units,
        fetchedAt= fetchedAt,
    )
}

private fun CurrentDto.toCurrentWeather() = CurrentWeather(
    time = LocalDateTime.parse(time),
    temperature = temperature,
    apparentTemperature = apparentTemperature,
    humidity = humidity,
    isDay = isDay == 1,
    precipitation = precipitation,
    weatherCode = weatherCode,
    cloudCover = cloudCover,
    pressure = pressure,
    windSpeed = windSpeed,
    windDirection = windDirection,
    windGusts = windGusts,
)

private fun HourlyDto.toHourlyList(firstHour: LocalDateTime): List<HourlyForecast> {
    val result = ArrayList<HourlyForecast>(time.size)
    for (i in time.indices) {
        val rowTime = LocalDateTime.parse(time[i])
        if (rowTime.isBefore(firstHour)) continue

        val temp = temperature.getOrNull(i) ?: continue
        result += HourlyForecast(
            time = rowTime,
            temperature = temp,
            apparentTemperature = apparentTemperature.getOrNull(i) ?: temp,
            precipitationProbability = precipitationProbability.getOrNull(i),
            weatherCode = weatherCode.getOrNull(i) ?: 0,
            windSpeed = windSpeed.getOrNull(i) ?: 0.0,
        )
    }
    return result
}

private fun DailyDto.toDailyList(): List<DailyForecast> {
    val result = ArrayList<DailyForecast>(time.size)
    for (i in time.indices) {
        val max = temperatureMax.getOrNull(i) ?: continue
        val min = temperatureMin.getOrNull(i) ?: continue
        result += DailyForecast(
            date = LocalDate.parse(time[i]),
            weatherCode = weatherCode.getOrNull(i) ?: 0,
            temperatureMax = max,
            temperatureMin = min,
            sunrise = sunrise.getOrNull(i)?.let(LocalDateTime::parse),
            sunset = sunset.getOrNull(i)?.let(LocalDateTime::parse),
            uvIndexMax = uvIndexMax.getOrNull(i),
            precipitationSum = precipitationSum.getOrNull(i) ?: 0.0,
            precipitationProbabilityMax = precipitationProbabilityMax.getOrNull(i),
            windSpeedMax = windSpeedMax.getOrNull(i) ?: 0.0,
        )
    }
    return result
}