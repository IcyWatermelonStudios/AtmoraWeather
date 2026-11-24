package com.icywatermelonstudios.atmoraweather.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.icywatermelonstudios.atmoraweather.WeatherApp
import com.icywatermelonstudios.atmoraweather.data.local.SettingsStore
import com.icywatermelonstudios.atmoraweather.ui.theme.AtmoraWeatherTheme
import kotlinx.coroutines.launch

// opened by the launcher when a widget is added and user selects settings when holding on a widget
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // if the user leaved without saving then it won't add the widget
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val store = (application as WeatherApp).container.settingsStore

        setContent {
            AtmoraWeatherTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    store = store,
                    onSaved = {
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),)
                        finish()
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    store: SettingsStore,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locations by store.locations.collectAsState(initial = emptyList())
    val homeId by store.homeLocationId.collectAsState(initial = null)
    var loaded by rememberSaveable { mutableStateOf(false) }
    var transparent by rememberSaveable { mutableStateOf(false) }
    var pickedId by rememberSaveable { mutableStateOf<Long?>(null) }

    // load the widget's current settings only ONCE please
    LaunchedEffect(appWidgetId) {
        if (!loaded) {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            transparent = prefs[WidgetKeys.TRANSPARENT] ?: false
            pickedId = prefs[WidgetKeys.LOCATION_ID]
            loaded = true
        }
    }

    val shownId = pickedId?.takeIf { id -> locations.any { it.id == id } }
        ?: homeId
        ?: locations.firstOrNull()?.id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget settings") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Background", style = MaterialTheme.typography.titleMedium)

            val options = listOf("Solid", "Transparent")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = (index == 1) == transparent,
                        onClick = { transparent = index == 1 },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        label = { Text(label) },
                    )
                }
            }
            Text(
                text = "Transparent uses white text, so it looks best on darker wallpapers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )

            if (locations.isEmpty()) {
                Text(
                    text = "No saved locations yet. Add one in the app first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            locations.forEach { location ->
                val isSelected = location.id == shownId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = { pickedId = location.id },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            text = if (location.id == homeId) "${location.name} (Home)" else location.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (location.subtitle.isNotEmpty()) {
                            Text(
                                text = location.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Text(
                text = "Until you pick a city yourself, the widget follows your home location.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                // disabled until the saved settings are loaded, so they cannot be overwritten with defaults
                enabled = loaded,
                onClick = {
                    scope.launch {
                        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                        updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[WidgetKeys.TRANSPARENT] = transparent
                            val picked = pickedId
                            if (picked != null) {
                                prefs[WidgetKeys.LOCATION_ID] = picked
                            } else {
                                prefs.remove(WidgetKeys.LOCATION_ID)
                            }
                        }
                        // redraw this widget with the new settings
                        WeatherWidget().update(context, glanceId)
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text("Save")
            }
        }
    }
}