package com.icywatermelonstudios.atmoraweather.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.icywatermelonstudios.atmoraweather.MainActivity
import com.icywatermelonstudios.atmoraweather.WeatherApp
import com.icywatermelonstudios.atmoraweather.data.model.WeatherCode
import com.icywatermelonstudios.atmoraweather.data.model.WeatherReport
import com.icywatermelonstudios.atmoraweather.data.toUserMessage
import com.icywatermelonstudios.atmoraweather.ui.screens.formatTemp
import com.icywatermelonstudios.atmoraweather.ui.screens.timeFormatter
import kotlinx.coroutines.flow.first

object WidgetKeys {
    // pinned location id
    val LOCATION_ID = longPreferencesKey("widget_location_id")
    // true means no bg false means solid bg
    val TRANSPARENT = booleanPreferencesKey("widget_transparent")
}

// widget states
private sealed interface WidgetState {
    data object NoLocation : WidgetState
    data class Error(val message: String) : WidgetState
    data class Ready(val report: WeatherReport) : WidgetState
}

// 2x1 overview widget
class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val transparent = prefs[WidgetKeys.TRANSPARENT] ?: false
        val state = loadState(context, prefs[WidgetKeys.LOCATION_ID])

        provideContent {
            GlanceTheme {
                WidgetContent(state, transparent)
            }
        }
    }

    private suspend fun loadState(context: Context, pinnedId: Long?): WidgetState {
        val container = (context.applicationContext as WeatherApp).container
        val store = container.settingsStore

        val locations = store.locations.first()
        val homeId = store.homeLocationId.first()

        // pinned city if it still exists. otherwise home
        val location = locations.firstOrNull { it.id == pinnedId } ?: locations.firstOrNull { it.id == homeId } ?: locations.firstOrNull() ?: return WidgetState.NoLocation

        val units = store.units.first()

        return container.repository.getWeather(location, units).fold(
            onSuccess = { WidgetState.Ready(it) },
            onFailure = { WidgetState.Error(it.toUserMessage()) },
        )
    }
}

// widget colors
private class WidgetColors(val primary: ColorProvider, val secondary: ColorProvider)

@SuppressLint("RestrictedApi")
@Composable
private fun widgetColors(transparent: Boolean): WidgetColors =
    if (transparent) {
        WidgetColors(
            primary = ColorProvider(Color.White),
            secondary = ColorProvider(Color.White.copy(alpha = 0.75f)),
        )
    } else {
        WidgetColors(
            primary = GlanceTheme.colors.onSurface,
            secondary = GlanceTheme.colors.onSurfaceVariant,
        )
    }

@Composable
private fun WidgetContent(state: WidgetState, transparent: Boolean) {
    val colors = widgetColors(transparent)

    val backgroundModifier = if (transparent) {
        GlanceModifier
    } else {
        GlanceModifier.background(GlanceTheme.colors.widgetBackground).cornerRadius(android.R.dimen.system_app_widget_background_radius)
    }

    Row(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground().then(backgroundModifier).clickable(
            androidx.glance.action.actionStartActivity<MainActivity>()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        when (state) {
            is WidgetState.NoLocation -> MessageText("Add a location in the app", colors)
            is WidgetState.Error -> MessageText("${state.message} Tap to open.", colors)
            is WidgetState.Ready -> ReadyContent(state.report, colors)
        }
    }
}

// the ui of the widget itself
@Composable
private fun RowScope.ReadyContent(report: WeatherReport, colors: WidgetColors) {
    // left column takes the free width and sticks to top
    Column(
        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        Text(
            text = report.location.name,
            maxLines = 1,
            style = TextStyle(color = colors.secondary, fontSize = 12.sp),
        )
        Text(
            text = formatTemp(report.current.temperature),
            maxLines = 1,
            style = TextStyle(
                color = colors.primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            ),
        )
    }

    // right column is vertically centered and right-aligned
    Column(
        modifier = GlanceModifier.fillMaxHeight(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.End,
    ) {
        Text(
            text = WeatherCode.describe(report.current.weatherCode),
            maxLines = 2,
            style = TextStyle(
                color = colors.primary,
                fontSize = 13.sp,
                textAlign = TextAlign.End,
            ),
        )
        Text(
            text = "Updated ${timeFormatter.format(report.fetchedAt)}",
            maxLines = 1,
            style = TextStyle(
                color = colors.secondary,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
            ),
        )
    }
}

@Composable
private fun MessageText(text: String, colors: WidgetColors) {
    Text(
        text = text,
        maxLines = 3,
        style = TextStyle(color = colors.primary, fontSize = 13.sp),
    )
}