package com.icywatermelonstudios.atmoraweather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.icywatermelonstudios.atmoraweather.MainActivity
import com.icywatermelonstudios.atmoraweather.WeatherApp
import com.icywatermelonstudios.atmoraweather.data.model.WeatherCode
import com.icywatermelonstudios.atmoraweather.data.model.WeatherReport
import com.icywatermelonstudios.atmoraweather.data.toUserMessage
import com.icywatermelonstudios.atmoraweather.ui.screens.formatTemp
import kotlinx.coroutines.flow.first

private sealed interface WidgetState {
    data object NoLocation : WidgetState
    data class Error(val message: String) : WidgetState
    data class Ready(val report: WeatherReport) : WidgetState
}

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)

        provideContent {
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }

    private suspend fun loadState(context: Context): WidgetState {
        val container = (context.applicationContext as WeatherApp).container
        val store = container.settingsStore
        val locations = store.locations.first()
        val homeId = store.homeLocationId.first()
        val location = locations.firstOrNull { it.id == homeId } ?: locations.firstOrNull()
        ?: return WidgetState.NoLocation

        val units = store.units.first()

        return container.repository.getWeather(location, units).fold(
            onSuccess = { WidgetState.Ready(it) },
            onFailure = { WidgetState.Error(it.toUserMessage()) },
        )
    }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        when (state) {
            is WidgetState.NoLocation -> MessageText("Add a location in the app")
            is WidgetState.Error -> MessageText("${state.message}\nTap to open the app")
            is WidgetState.Ready -> ReadyContent(state.report)
        }
    }
}

@Composable
private fun ReadyContent(report: WeatherReport) {
    val current = report.current
    val today = report.daily.firstOrNull()

    Text(
        text = report.location.name,
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = 13.sp,
        ),
    )
    Text(
        text = formatTemp(current.temperature),
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 38.sp,
            fontWeight = FontWeight.Medium,
        ),
    )
    Text(
        text = WeatherCode.describe(current.weatherCode),
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 13.sp,
        ),
    )
    if (today != null) {
        Text(
            text = "High ${formatTemp(today.temperatureMax)}  ·  Low ${formatTemp(today.temperatureMin)}",
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
private fun MessageText(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 14.sp,
        ),
    )
}