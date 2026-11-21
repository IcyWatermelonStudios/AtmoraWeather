package com.icywatermelonstudios.atmoraweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.icywatermelonstudios.atmoraweather.data.model.Location

// dialog for finding a city and adding it to the saved locations. it's stateless so the text results and etc all come from SearchState
@Composable
fun AddLocationDialog(
    state: SearchState,
    onQueryChange: (String) -> Unit,
    onSelect: (Location) -> Unit,
    onDismiss: () -> Unit,
) {
    // used to put the cursor into the text field
    var focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add location") },
        text = {
            Column {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = { Text("City name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )

                // below the field exactly one of these is shown
                when {
                    state.isSearching -> CircularProgressIndicator(
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    state.error != null -> Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    state.noResults -> Text(
                        text = "No cities found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    else -> LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp).padding(top = 8.dp),
                    ) {
                        items(state.results, key = { it.id }) { location ->
                            ListItem(
                                headlineContent = { Text(location.name) },
                                supportingContent = {
                                    if (location.subtitle.isNotEmpty()) Text(location.subtitle)
                                },
                                // the dialog already has its own background so row stays transparent
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onSelect(location) },
                            )
                        }
                    }
                }
            }
        },
        // alert dialog requires a confirm button slot, so it'll just be cancel cuz i got no OK
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}