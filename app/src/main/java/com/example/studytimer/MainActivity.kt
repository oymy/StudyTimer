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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.studytimer.ui.theme.StudyTimerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
                // Collect state from service if bound, otherwise use local UI state
                val serviceTimerState = studyTimerService?.timerState?.collectAsState()
                val serviceTimeLeftInSession = studyTimerService?.timeLeftInSession?.collectAsState()
                val serviceTimeUntilNextAlarm = studyTimerService?.timeUntilNextAlarm?.collectAsState()
                
                val uiTimerStateState = uiTimerState.collectAsState()
                val uiTimeLeftInSessionState = uiTimeLeftInSession.collectAsState()
                val uiTimeUntilNextAlarmState = uiTimeUntilNextAlarm.collectAsState()
                
                // Settings state
                val studyDurationState = studyDurationMin.collectAsState()
                val minAlarmIntervalState = minAlarmIntervalMin.collectAsState()
                val maxAlarmIntervalState = maxAlarmIntervalMin.collectAsState()
                
                // Use service state if available, otherwise use UI state
                val timerState = serviceTimerState?.value ?: uiTimerStateState.value
                val timeLeftInSession = serviceTimeLeftInSession?.value ?: uiTimeLeftInSessionState.value
                val timeUntilNextAlarm = serviceTimeUntilNextAlarm?.value ?: uiTimeUntilNextAlarmState.value
                
                StudyTimerApp(
                    timerState = timerState,
                    timeLeftInSession = timeLeftInSession,
                    timeUntilNextAlarm = timeUntilNextAlarm,
                    studyDurationMin = studyDurationState.value,
                    minAlarmIntervalMin = minAlarmIntervalState.value,
                    maxAlarmIntervalMin = maxAlarmIntervalState.value,
                    onStudyDurationChange = { _studyDurationMin.value = it },
                    onMinAlarmIntervalChange = { _minAlarmIntervalMin.value = it },
                    onMaxAlarmIntervalChange = { _maxAlarmIntervalMin.value = it },
                    onStartClick = { startStudySession() },
                    onStopClick = { stopStudySession() }
                )
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
    
    // Settings state flows
    private val _studyDurationMin = MutableStateFlow(90) // Default 90 minutes
    private val _minAlarmIntervalMin = MutableStateFlow(3) // Default 3 minutes
    private val _maxAlarmIntervalMin = MutableStateFlow(5) // Default 5 minutes
    
    val uiTimerState: StateFlow<StudyTimerService.TimerState> = _uiTimerState
    val uiTimeLeftInSession: StateFlow<Long> = _uiTimeLeftInSession
    val uiTimeUntilNextAlarm: StateFlow<Long> = _uiTimeUntilNextAlarm
    val studyDurationMin: StateFlow<Int> = _studyDurationMin
    val minAlarmIntervalMin: StateFlow<Int> = _minAlarmIntervalMin
    val maxAlarmIntervalMin: StateFlow<Int> = _maxAlarmIntervalMin
    
    private fun startStudySession() {
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_START
            putExtra(StudyTimerService.EXTRA_STUDY_DURATION_MIN, _studyDurationMin.value)
            putExtra(StudyTimerService.EXTRA_MIN_ALARM_INTERVAL_MIN, _minAlarmIntervalMin.value)
            putExtra(StudyTimerService.EXTRA_MAX_ALARM_INTERVAL_MIN, _maxAlarmIntervalMin.value)
        }
        
        // Immediately update UI state
        _uiTimerState.value = StudyTimerService.TimerState.STUDYING
        _uiTimeLeftInSession.value = _studyDurationMin.value * 60 * 1000L // Convert minutes to ms
        _uiTimeUntilNextAlarm.value = _minAlarmIntervalMin.value * 60 * 1000L // Initial alarm time
        
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

@Composable
fun StudyTimerApp(
    timerState: StudyTimerService.TimerState,
    timeLeftInSession: Long,
    timeUntilNextAlarm: Long,
    studyDurationMin: Int,
    minAlarmIntervalMin: Int,
    maxAlarmIntervalMin: Int,
    onStudyDurationChange: (Int) -> Unit,
    onMinAlarmIntervalChange: (Int) -> Unit,
    onMaxAlarmIntervalChange: (Int) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // App title
                Text(
                    text = "Study Timer",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Timer display
                TimerDisplay(
                    timerState = timerState,
                    timeLeftInSession = timeLeftInSession,
                    timeUntilNextAlarm = timeUntilNextAlarm
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Settings section - only show when timer is idle
                if (timerState == StudyTimerService.TimerState.IDLE) {
                    SettingsSection(
                        studyDurationMin = studyDurationMin,
                        minAlarmIntervalMin = minAlarmIntervalMin,
                        maxAlarmIntervalMin = maxAlarmIntervalMin,
                        onStudyDurationChange = onStudyDurationChange,
                        onMinAlarmIntervalChange = onMinAlarmIntervalChange,
                        onMaxAlarmIntervalChange = onMaxAlarmIntervalChange
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Control buttons
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onStartClick,
                            enabled = timerState == StudyTimerService.TimerState.IDLE,
                            modifier = Modifier.size(width = 140.dp, height = 56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color.Gray
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Start", 
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Button(
                            onClick = onStopClick,
                            enabled = timerState != StudyTimerService.TimerState.IDLE,
                            modifier = Modifier.size(width = 140.dp, height = 56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE57373),
                                disabledContainerColor = Color.Gray
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop",
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Stop", 
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerDisplay(
    timerState: StudyTimerService.TimerState,
    timeLeftInSession: Long,
    timeUntilNextAlarm: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (timerState) {
                StudyTimerService.TimerState.STUDYING -> Color(0xFFE3F2FD)
                StudyTimerService.TimerState.BREAK -> Color(0xFFE8F5E9)
                StudyTimerService.TimerState.EYE_REST -> Color(0xFFFFF3E0)
                StudyTimerService.TimerState.IDLE -> Color(0xFFF5F5F5)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status text
            Text(
                text = when (timerState) {
                    StudyTimerService.TimerState.STUDYING -> "Studying"
                    StudyTimerService.TimerState.BREAK -> "Taking a Break"
                    StudyTimerService.TimerState.EYE_REST -> "Rest Your Eyes"
                    StudyTimerService.TimerState.IDLE -> "Ready to Start"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main timer
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                if (timerState != StudyTimerService.TimerState.IDLE) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        progress = when (timerState) {
                            StudyTimerService.TimerState.STUDYING -> {
                                val totalTime = 90 * 60 * 1000L
                                1f - (timeLeftInSession.toFloat() / totalTime)
                            }
                            StudyTimerService.TimerState.BREAK -> {
                                val totalTime = 20 * 60 * 1000L
                                1f - (timeLeftInSession.toFloat() / totalTime)
                            }
                            StudyTimerService.TimerState.EYE_REST -> {
                                val totalTime = 10 * 1000L
                                1f - (timeLeftInSession.toFloat() / totalTime)
                            }
                            StudyTimerService.TimerState.IDLE -> 0f
                        },
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(timeLeftInSession),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (timerState == StudyTimerService.TimerState.STUDYING) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Next Alarm",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = " Next: ${formatTime(timeUntilNextAlarm)}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructions
            Text(
                text = when (timerState) {
                    StudyTimerService.TimerState.STUDYING -> 
                        "Focus on your work. Alarms will sound every 3-5 minutes."
                    StudyTimerService.TimerState.BREAK -> 
                        "Take a break! You've earned it."
                    StudyTimerService.TimerState.EYE_REST -> 
                        "Close your eyes and relax for 10 seconds."
                    StudyTimerService.TimerState.IDLE -> 
                        "90min study + 20min break cycles with eye rest alarms every 3-5min."
                },
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

// Helper function to format time
private fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun SettingsSection(
    studyDurationMin: Int,
    minAlarmIntervalMin: Int,
    maxAlarmIntervalMin: Int,
    onStudyDurationChange: (Int) -> Unit,
    onMinAlarmIntervalChange: (Int) -> Unit,
    onMaxAlarmIntervalChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Study Duration Setting
            SettingItem(
                title = "Study Duration",
                value = "$studyDurationMin min",
                options = listOf(30, 60, 90, 120),
                formatOption = { "$it min" },
                onOptionSelected = onStudyDurationChange
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Alarm Interval Settings
            Text(
                text = "Alarm Interval: $minAlarmIntervalMin - $maxAlarmIntervalMin min",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Min Alarm Interval
            SettingItem(
                title = "Minimum",
                value = "$minAlarmIntervalMin min",
                options = listOf(1, 2, 3, 5, 7, 10),
                formatOption = { "$it min" },
                onOptionSelected = { newMin ->
                    onMinAlarmIntervalChange(newMin)
                    // Ensure max is greater than min
                    if (newMin >= maxAlarmIntervalMin) {
                        onMaxAlarmIntervalChange(newMin + 1)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Max Alarm Interval
            SettingItem(
                title = "Maximum",
                value = "$maxAlarmIntervalMin min",
                options = listOf(2, 5, 7, 10, 15, 20),
                formatOption = { "$it min" },
                onOptionSelected = { newMax ->
                    // Ensure max is greater than min
                    if (newMax > minAlarmIntervalMin) {
                        onMaxAlarmIntervalChange(newMax)
                    }
                }
            )
        }
    }
}

@Composable
fun <T> SettingItem(
    title: String,
    value: String,
    options: List<T>,
    formatOption: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        
        Box {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.width(100.dp)
            ) {
                Text(text = value)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(100.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(formatOption(option)) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudyTimerPreview() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.IDLE,
            timeLeftInSession = 45 * 60 * 1000L, // 45 minutes
            timeUntilNextAlarm = 2 * 60 * 1000L, // 2 minutes
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            onStudyDurationChange = {},
            onMinAlarmIntervalChange = {},
            onMaxAlarmIntervalChange = {},
            onStartClick = {},
            onStopClick = {}
        )
    }
}