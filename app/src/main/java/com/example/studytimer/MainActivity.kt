package com.example.studytimer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.studytimer.ui.theme.StudyTimerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var studyTimerService: StudyTimerService? = null
    private var bound = false
    
    // Service connection
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StudyTimerService.LocalBinder
            studyTimerService = binder.getService()
            bound = true
            Log.d(TAG, "Service connected")
            
            // Update UI with current service state
            lifecycleScope.launch {
                updateUIFromService()
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            studyTimerService = null
            bound = false
            Log.d(TAG, "Service disconnected")
        }
    }
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            StudyTimerTheme {
                val showSettings by _showSettings.collectAsState()
                
                if (showSettings) {
                    // Show Settings Screen
                    SettingsScreen(
                        studyDurationMin = _studyDurationMin.value,
                        minAlarmIntervalMin = _minAlarmIntervalMin.value,
                        maxAlarmIntervalMin = _maxAlarmIntervalMin.value,
                        onStudyDurationChange = { _studyDurationMin.value = it },
                        onMinAlarmIntervalChange = { _minAlarmIntervalMin.value = it },
                        onMaxAlarmIntervalChange = { _maxAlarmIntervalMin.value = it },
                        onNavigateBack = { _showSettings.value = false }
                    )
                } else {
                    // Collect state from service if bound, otherwise use local UI state
                    val serviceTimerState = studyTimerService?.timerState?.collectAsState()
                    val serviceTimeLeftInSession = studyTimerService?.timeLeftInSession?.collectAsState()
                    val serviceTimeUntilNextAlarm = studyTimerService?.timeUntilNextAlarm?.collectAsState()
                    
                    val uiTimerStateState = uiTimerState.collectAsState()
                    val uiTimeLeftInSessionState = uiTimeLeftInSession.collectAsState()
                    val uiTimeUntilNextAlarmState = uiTimeUntilNextAlarm.collectAsState()
                    
                    // Use service state if available, otherwise use UI state
                    val timerState = serviceTimerState?.value ?: uiTimerStateState.value
                    val timeLeftInSession = serviceTimeLeftInSession?.value ?: uiTimeLeftInSessionState.value
                    val timeUntilNextAlarm = serviceTimeUntilNextAlarm?.value ?: uiTimeUntilNextAlarmState.value
                    
                    StudyTimerApp(
                        timerState = timerState,
                        timeLeftInSession = timeLeftInSession,
                        timeUntilNextAlarm = timeUntilNextAlarm,
                        onStartClick = { startStudySession() },
                        onStopClick = { stopStudySession() },
                        onSettingsClick = { _showSettings.value = true } // Navigate to settings
                    )
                }
            }
        }
    }
    
    // Coroutine job references to cancel previous collectors
    private var timerStateJob: Job? = null
    private var timeLeftJob: Job? = null
    private var alarmTimeJob: Job? = null
    
    private fun updateUIFromService() {
        // Cancel any existing collectors
        timerStateJob?.cancel()
        timeLeftJob?.cancel()
        alarmTimeJob?.cancel()
        
        // Update local UI state from service when connected
        studyTimerService?.let { service ->
            // Collect timer state
            timerStateJob = lifecycleScope.launch {
                service.timerState.collect { state ->
                    _uiTimerState.value = state
                    
                    // If we just entered eye rest state, show a toast notification
                    if (state == StudyTimerService.TimerState.EYE_REST) {
                        Toast.makeText(
                            this@MainActivity,
                            "Rest your eyes for 10 seconds",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            
            // Collect time left in session
            timeLeftJob = lifecycleScope.launch {
                service.timeLeftInSession.collect { time ->
                    _uiTimeLeftInSession.value = time
                }
            }
            
            // Collect time until next alarm
            alarmTimeJob = lifecycleScope.launch {
                service.timeUntilNextAlarm.collect { time ->
                    _uiTimeUntilNextAlarm.value = time
                }
            }
        }
    }
    
    // State flows to maintain UI state when service is not bound
    private val _uiTimerState = MutableStateFlow(StudyTimerService.TimerState.IDLE)
    private val _uiTimeLeftInSession = MutableStateFlow(0L)
    private val _uiTimeUntilNextAlarm = MutableStateFlow(0L)
    
    // Settings state flows - needed for both service and settings screen
    private val _studyDurationMin = MutableStateFlow(90) // Default 90 minutes
    private val _minAlarmIntervalMin = MutableStateFlow(3) // Default 3 minutes
    private val _maxAlarmIntervalMin = MutableStateFlow(5) // Default 5 minutes
    
    // Navigation state flow
    private val _showSettings = MutableStateFlow(false)
    
    val uiTimerState: StateFlow<StudyTimerService.TimerState> = _uiTimerState
    val uiTimeLeftInSession: StateFlow<Long> = _uiTimeLeftInSession
    val uiTimeUntilNextAlarm: StateFlow<Long> = _uiTimeUntilNextAlarm
    
    private fun startStudySession() {
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_START
            putExtra(StudyTimerService.EXTRA_STUDY_DURATION_MIN, _studyDurationMin.value)
            putExtra(StudyTimerService.EXTRA_MIN_ALARM_INTERVAL_MIN, _minAlarmIntervalMin.value)
            putExtra(StudyTimerService.EXTRA_MAX_ALARM_INTERVAL_MIN, _maxAlarmIntervalMin.value)
        }
        
        // Immediately update UI state
        _uiTimerState.value = StudyTimerService.TimerState.STUDYING
        _uiTimeLeftInSession.value = 90 * 60 * 1000L // Convert minutes to ms
        _uiTimeUntilNextAlarm.value = 3 * 60 * 1000L // Initial alarm time
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun stopStudySession() {
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_STOP
        }
        startService(intent)
        
        // Immediately update UI state
        _uiTimerState.value = StudyTimerService.TimerState.IDLE
        _uiTimeLeftInSession.value = 0L
        _uiTimeUntilNextAlarm.value = 0L
    }
    
    override fun onStart() {
        super.onStart()
        // Bind to the service
        Intent(this, StudyTimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }
    
    override fun onStop() {
        super.onStop()
        // Unbind from the service
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}