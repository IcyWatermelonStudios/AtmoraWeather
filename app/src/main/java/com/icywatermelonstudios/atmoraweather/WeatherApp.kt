package com.icywatermelonstudios.atmoraweather

import android.app.Application
import android.content.Context
import com.icywatermelonstudios.atmoraweather.data.WeatherRepository
import com.icywatermelonstudios.atmoraweather.data.local.SettingsStore
import com.icywatermelonstudios.atmoraweather.data.remote.ApiFactory

// holds the objects of the app, each one is created once
class AppContainer(context: Context) {

    private val apiFactory = ApiFactory()

    val settingsStore: SettingsStore by lazy { SettingsStore(context) }

    val repository: WeatherRepository by lazy {
        WeatherRepository(
            forecastApi = apiFactory.forecastApi,
            geocodingApi = apiFactory.geocodingApi,
        )
    }
}

// registered in android manifest thru android:name=".WeatherApp"
// android creates before any activity
class WeatherApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // applicationContext is passed on purpose: it lives as long as the process
        // so the container can never leak an Activity
        container = AppContainer(applicationContext)
    }
}