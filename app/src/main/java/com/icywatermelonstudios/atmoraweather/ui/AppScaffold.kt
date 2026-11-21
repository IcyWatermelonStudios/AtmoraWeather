package com.icywatermelonstudios.atmoraweather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.icywatermelonstudios.atmoraweather.data.model.Location
import com.icywatermelonstudios.atmoraweather.ui.screens.DailyScreen
import com.icywatermelonstudios.atmoraweather.ui.screens.HourlyScreen
import com.icywatermelonstudios.atmoraweather.ui.screens.OverviewScreen
import com.icywatermelonstudios.atmoraweather.ui.screens.SettingsScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val NAV_ANIM_MS = 300

// bottom bar destinations
private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Overview("overview", "Overview", Icons.Outlined.Dashboard),
    Daily("daily", "Daily", Icons.Outlined.CalendarMonth),
    Hourly("hourly", "Hourly", Icons.Outlined.Schedule),
    Settings("settings", "Settings", Icons.Outlined.Settings),
}

private val bottomDestinations = listOf(Destination.Overview, Destination.Daily, Destination.Hourly)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(viewModel: MainViewModel) {
    // collectAsStateWithLifecycle stops collecting when app is in bg
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val search by viewModel.searchState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // rememberSaveable keeps the dialog open across rotation
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isSettings = currentRoute == Destination.Settings.route

    val hasLocation = state.selectedLocation != null

    // top bar title, the city name or settings for settings view
    val title = if (currentRoute == Destination.Settings.route) {
        "Settings"
    } else {
        state.selectedLocation?.name ?: "Atmora Weather"
    }

    // ModalNavigationDrawer provides the swipe from left edge thing for free
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isSettings,
        drawerContent = {
            LocationsDrawer(
                locations = state.locations,
                selectedId = state.selectedLocation?.id,
                homeId = state.homeLocationId,
                onSelect = { id ->
                    viewModel.selectLocation(id)
                    scope.launch { drawerState.close() }
                },
                onSetHome = viewModel::setHomeLocation,
                onRemove = viewModel::removeLocation,
                onAdd = {
                    scope.launch { drawerState.close() }
                    showAddDialog = true
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    // the menu button in the top left open the drawer
                    navigationIcon = {
                        if (isSettings) {
                            // on the settings screen the left button becomes back
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open locations menu")
                            }
                        }
                    },
                    actions = {
                        // hidden when there is no location
                        if (!isSettings && hasLocation) {
                            IconButton(
                                onClick = {
                                    navController.navigate(Destination.Settings.route) {
                                        launchSingleTop = true
                                    }
                                },
                            ) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                // no point in navigation until there is a location to show
                if (hasLocation && !isSettings) {
                    NavigationBar {
                        bottomDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        // keep a single back stack root and restore each tabs state
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when {
                    // saved settings are still loading, show nothing
                    !state.isReady -> Unit
                    !hasLocation -> NoLocationContent(onAdd = { showAddDialog = true })

                    else -> NavHost(
                        navController = navController,
                        startDestination = Destination.Overview.route,

                        enterTransition = {
                            if (targetState.destination.route == Destination.Settings.route) {
                                slideInHorizontally(tween(NAV_ANIM_MS)) { fullWidth -> fullWidth }
                            } else {
                                fadeIn(tween(NAV_ANIM_MS))
                            }
                        },

                        exitTransition = {
                            if (targetState.destination.route == Destination.Settings.route) {
                                slideOutHorizontally(tween(NAV_ANIM_MS)) { fullWidth -> -fullWidth / 4 } +
                                        fadeOut(tween(NAV_ANIM_MS))
                            } else {
                                fadeOut(tween(NAV_ANIM_MS))
                            }
                        },

                        popEnterTransition = {
                            if (initialState.destination.route == Destination.Settings.route) {
                                slideInHorizontally(tween(NAV_ANIM_MS)) { fullWidth -> -fullWidth / 4 } +
                                        fadeIn(tween(NAV_ANIM_MS))
                            } else {
                                fadeIn(tween(NAV_ANIM_MS))
                            }
                        },

                        popExitTransition = {
                            if (initialState.destination.route == Destination.Settings.route) {
                                slideOutHorizontally(tween(NAV_ANIM_MS)) { fullWidth -> fullWidth }
                            } else {
                                fadeOut(tween(NAV_ANIM_MS))
                            }
                        },
                    ) {
                        composable(Destination.Overview.route) {
                            OverviewScreen(weather = state.weather, onRefresh = viewModel::refresh)
                        }
                        composable(Destination.Daily.route) {
                            DailyScreen(weather = state.weather, onRefresh = viewModel::refresh)
                        }
                        composable(Destination.Hourly.route) {
                            HourlyScreen(weather = state.weather, onRefresh = viewModel::refresh)
                        }
                        composable(Destination.Settings.route) {
                            SettingsScreen(units = state.units, onUnitsChange = viewModel::setUnits)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLocationDialog(
            state = search,
            onQueryChange = viewModel::onSearchQueryChange,
            onSelect = { location ->
                viewModel.addLocation(location)
                showAddDialog = false
            },
            onDismiss = {
                viewModel.clearSearch()
                showAddDialog = false
            },
        )
    }
}

// content of the side menu
@Composable
private fun LocationsDrawer(
    locations: List<Location>,
    selectedId: Long?,
    homeId: Long?,
    onSelect: (Long) -> Unit,
    onSetHome: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = "Locations",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
        )

        // weight(1f) lets the list take the free space and scroll
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            locations.forEach { location ->
                val isHome = location.id == homeId

                NavigationDrawerItem(
                    label = {
                        Column {
                            Text(location.name, style = MaterialTheme.typography.bodyLarge)
                            if (location.subtitle.isNotEmpty()) {
                                Text(
                                    text = location.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    selected = location.id == selectedId,
                    onClick = { onSelect(location.id) },
                    // two small actions at then end of the row, mark as home and remove
                    badge = {
                        LocationRowActions(location, isHome, onSetHome, onRemove)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }

        HorizontalDivider()

        NavigationDrawerItem(
            label = { Text("Add location") },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            selected = false,
            onClick = onAdd,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }
}

@Composable
private fun LocationRowActions(
    location: Location,
    isHome: Boolean,
    onSetHome: (Long) -> Unit,
    onRemove: (Long) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(onClick = { onSetHome(location.id) }, enabled = !isHome) {
            Icon(
                imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                contentDescription = if (isHome) "Home location" else "Set as home",
            )
        }
        IconButton(onClick = { onRemove(location.id) }) {
            Icon(Icons.Outlined.Delete, contentDescription = "Remove location")
        }
    }
}

@Composable
private fun NoLocationContent(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No location yet",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Add your home location to see the forecast.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onAdd) {
            Text("Add location")
        }
    }
}