package com.example.palipat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.palipat.service.StorageService
import com.example.palipat.ui.TimerApp
import com.example.palipat.ui.TimerViewModel
import com.example.palipat.ui.theme.PalipatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val storageService = StorageService(this)
        val viewModelFactory = TimerViewModelFactory(storageService)
        val viewModel: TimerViewModel by viewModels { viewModelFactory }

        setContent {
            PalipatTheme {
                TimerApp(viewModel)
            }
        }
    }
}

class TimerViewModelFactory(private val storageService: StorageService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimerViewModel(storageService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
