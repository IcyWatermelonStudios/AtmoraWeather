package com.icywatermelonstudios.atmoraweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icywatermelonstudios.atmoraweather.ui.AppScaffold
import com.icywatermelonstudios.atmoraweather.ui.MainViewModel
import com.icywatermelonstudios.atmoraweather.ui.theme.AtmoraWeatherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AtmoraWeatherTheme {
                val viewModel: MainViewModel = viewModel()
                AppScaffold(viewModel = viewModel)
            }
        }
    }
}