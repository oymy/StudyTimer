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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
                
                // Use service state if available, otherwise use UI state
                val timerState = serviceTimerState?.value ?: uiTimerStateState.value
                val timeLeftInSession = serviceTimeLeftInSession?.value ?: uiTimeLeftInSessionState.value
                val timeUntilNextAlarm = serviceTimeUntilNextAlarm?.value ?: uiTimeUntilNextAlarmState.value
                
                StudyTimerApp(
                    timerState = timerState,
                    timeLeftInSession = timeLeftInSession,
                    timeUntilNextAlarm = timeUntilNextAlarm,
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
    
    val uiTimerState: StateFlow<StudyTimerService.TimerState> = _uiTimerState
    val uiTimeLeftInSession: StateFlow<Long> = _uiTimeLeftInSession
    val uiTimeUntilNextAlarm: StateFlow<Long> = _uiTimeUntilNextAlarm
    
    private fun startStudySession() {
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_START
        }
        
        // Immediately update UI state
        _uiTimerState.value = StudyTimerService.TimerState.STUDYING
        _uiTimeLeftInSession.value = 90 * 60 * 1000L // 90 minutes
        _uiTimeUntilNextAlarm.value = 3 * 60 * 1000L // Initial 3 minutes
        
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
                verticalArrangement = Arrangement.Center
            ) {
                // App title
                Text(
                    text = "Study Timer",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Timer display
                TimerDisplay(
                    timerState = timerState,
                    timeLeftInSession = timeLeftInSession,
                    timeUntilNextAlarm = timeUntilNextAlarm
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onStartClick,
                        enabled = timerState == StudyTimerService.TimerState.IDLE,
                        modifier = Modifier.size(width = 120.dp, height = 48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start"
                            )
                            Text(text = "Start", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    
                    Button(
                        onClick = onStopClick,
                        enabled = timerState != StudyTimerService.TimerState.IDLE,
                        modifier = Modifier.size(width = 120.dp, height = 48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop"
                            )
                            Text(text = "Stop", modifier = Modifier.padding(start = 8.dp))
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

@Preview(showBackground = true)
@Composable
fun StudyTimerPreview() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.STUDYING,
            timeLeftInSession = 45 * 60 * 1000L, // 45 minutes
            timeUntilNextAlarm = 2 * 60 * 1000L, // 2 minutes
            onStartClick = {},
            onStopClick = {}
        )
    }
}