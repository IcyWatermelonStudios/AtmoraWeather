package com.icywatermelonstudios.atmoraweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.icywatermelonstudios.atmoraweather.data.model.WeatherReport
import com.icywatermelonstudios.atmoraweather.ui.WeatherState

// shared wrapper for the overview daily and hourly views
// it decides what to show for the current state
// no report and error -> full screen error
// no report and loading -> loading indicator
// report available -> screen's own content
// the screen's content must be scrollable
// otherwise pull to refresh has nothing to react to

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateHost(
    weather: WeatherState,
    onRefresh: () -> Unit,
    content: @Composable (WeatherReport) -> Unit,
) {
    val report = weather.report
    val error = weather.error

    when {
        report == null && error != null -> ErrorContent(message = error, onRetry = onRefresh)

        report == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        else -> PullToRefreshBox(
            isRefreshing = weather.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(Modifier.fillMaxSize()) {
                // old data stays visible
                if (error != null) {
                    ErrorBanner(message = error, onRetry = onRefresh)
                }
                // weight(1f) lets the content the remaining height below
                Box(Modifier.weight(1f)) {
                    content(report)
                }
            }
        }
    }
}

// full screen error
@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Try again")
        }
    }
}

// compact error strip above the data
@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}