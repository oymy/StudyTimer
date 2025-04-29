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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.studytimer.ui.theme.StudyTimerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
        
        // Load settings from SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _studyDurationMin.value = prefs.getInt(KEY_STUDY_DURATION, 90)
        _minAlarmIntervalMin.value = prefs.getInt(KEY_MIN_ALARM, 3)
        _maxAlarmIntervalMin.value = prefs.getInt(KEY_MAX_ALARM, 5)
        _showNextAlarmTime.value = prefs.getBoolean(KEY_SHOW_NEXT_ALARM, false)
        
        setContent {
            StudyTimerTheme {
                val showSettings by _showSettings.collectAsState()
                
                if (showSettings) {
                    // Show Settings Screen
                    SettingsScreen(
                        studyDurationFlow = _studyDurationMin,
                        minAlarmIntervalFlow = _minAlarmIntervalMin,
                        maxAlarmIntervalFlow = _maxAlarmIntervalMin,
                        showNextAlarmTimeFlow = _showNextAlarmTime,
                        onStudyDurationChange = { newDuration ->
                            // Validate: Study duration >= min/max intervals
                            val minInterval = _minAlarmIntervalMin.value
                            val maxInterval = _maxAlarmIntervalMin.value
                            if (newDuration >= minInterval && newDuration >= maxInterval) {
                                _studyDurationMin.value = newDuration
                            }
                        },
                        onMinAlarmIntervalChange = { newMinInterval ->
                            // Validate: Min interval <= max interval && Min interval <= study duration
                            val maxInterval = _maxAlarmIntervalMin.value
                            val studyDuration = _studyDurationMin.value
                            if (newMinInterval <= maxInterval && newMinInterval <= studyDuration) {
                                _minAlarmIntervalMin.value = newMinInterval
                            }
                        },
                        onMaxAlarmIntervalChange = { newMaxInterval ->
                            // Validate: Max interval >= min interval && Max interval <= study duration
                            val minInterval = _minAlarmIntervalMin.value
                            val studyDuration = _studyDurationMin.value
                            if (newMaxInterval >= minInterval && newMaxInterval <= studyDuration) {
                                _maxAlarmIntervalMin.value = newMaxInterval
                            }
                        },
                        onShowNextAlarmTimeChange = { show ->
                            _showNextAlarmTime.value = show
                        },
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
                    val showNextAlarm = showNextAlarmTime.collectAsState().value // Collect the setting state
                    
                    StudyTimerApp(
                        timerState = timerState,
                        timeLeftInSession = timeLeftInSession,
                        timeUntilNextAlarm = timeUntilNextAlarm,
                        showNextAlarmTime = showNextAlarm, // Pass the boolean value
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
    private val _showNextAlarmTime = MutableStateFlow(false) // Default to false
    
    // Navigation state flow
    private val _showSettings = MutableStateFlow(false)
    
    val uiTimerState: StateFlow<StudyTimerService.TimerState> = _uiTimerState
    val uiTimeLeftInSession: StateFlow<Long> = _uiTimeLeftInSession
    val uiTimeUntilNextAlarm: StateFlow<Long> = _uiTimeUntilNextAlarm
    val showNextAlarmTime: StateFlow<Boolean> = _showNextAlarmTime
    
    private fun startStudySession() {
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_START
            putExtra(StudyTimerService.EXTRA_STUDY_DURATION_MIN, _studyDurationMin.value)
            putExtra(StudyTimerService.EXTRA_MIN_ALARM_INTERVAL_MIN, _minAlarmIntervalMin.value)
            putExtra(StudyTimerService.EXTRA_MAX_ALARM_INTERVAL_MIN, _maxAlarmIntervalMin.value)
            putExtra(StudyTimerService.EXTRA_SHOW_NEXT_ALARM_TIME, _showNextAlarmTime.value)
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
        // Persist settings just in case they were changed but not saved via the settings screen lambdas ( belt-and-suspenders)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit() 
            .putInt(KEY_STUDY_DURATION, _studyDurationMin.value)
            .putInt(KEY_MIN_ALARM, _minAlarmIntervalMin.value)
            .putInt(KEY_MAX_ALARM, _maxAlarmIntervalMin.value)
            .putBoolean(KEY_SHOW_NEXT_ALARM, _showNextAlarmTime.value)
            .apply()
        
        // Unbind from the service
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "StudyTimerPrefs"
        private const val KEY_STUDY_DURATION = "studyDuration"
        private const val KEY_MIN_ALARM = "minAlarmInterval"
        private const val KEY_MAX_ALARM = "maxAlarmInterval"
        private const val KEY_SHOW_NEXT_ALARM = "showNextAlarmTime"
    }
}