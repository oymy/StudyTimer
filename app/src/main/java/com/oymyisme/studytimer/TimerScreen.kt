package com.oymyisme.studytimer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.TimeUnit
import com.oymyisme.studytimer.ui.theme.StudyTimerTheme
import java.util.Locale

/**
 * The main UI composable for the Timer screen.
 */
@Composable
fun StudyTimerApp(
    timerState: StudyTimerService.TimerState,
    timeLeftInSession: Long,
    timeUntilNextAlarm: Long,
    showNextAlarmTime: Boolean,
    studyDurationMin: Int,
    minAlarmIntervalMin: Int,
    maxAlarmIntervalMin: Int,
    breakDurationMin: Int,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: () -> Unit
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
                
                // Timer Display (CircularProgressIndicator + Text)
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { // Full circle when idle
                            // For eye rest, show progress decreasing from full
                            // Avoid division by zero, progress is 1f anyway
                            // Calculate progress based on the current state and its duration
                            // Calculate progress based on the current state and its duration
                            val totalDurationMs = when (timerState) {
                                StudyTimerService.TimerState.STUDYING -> studyDurationMin * 60 * 1000L
                                StudyTimerService.TimerState.BREAK -> breakDurationMin * 60 * 1000L
                                StudyTimerService.TimerState.EYE_REST -> StudyTimerService.EYE_REST_TIME_MS
                                StudyTimerService.TimerState.IDLE -> 1L // Avoid division by zero, progress is 1f anyway
                            }// Full circle when idle
                            // For eye rest, show progress decreasing from full
                            // Avoid division by zero, progress is 1f anyway
                            // Calculate progress based on the current state and its duration
                            if (totalDurationMs > 0 && (timerState != StudyTimerService.TimerState.IDLE && timerState != StudyTimerService.TimerState.EYE_REST)) {
                                (timeLeftInSession.toFloat() / totalDurationMs.toFloat())
                            } else if (timerState == StudyTimerService.TimerState.EYE_REST) {
                                // For eye rest, show progress decreasing from full
                                (timeLeftInSession.toFloat() / StudyTimerService.EYE_REST_TIME_MS.toFloat())
                            } else {
                                1f // Full circle when idle
                            }
                        },
                        modifier = Modifier.size(200.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 12.dp,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(timeLeftInSession),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Conditionally show the next alarm time row
                        if (timerState == StudyTimerService.TimerState.STUDYING && showNextAlarmTime) {
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
                            "${studyDurationMin}min study + ${breakDurationMin}min break cycles with eye rest alarms every ${minAlarmIntervalMin}-${maxAlarmIntervalMin}min."
                    },
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Settings button - only show when timer is idle
                if (timerState == StudyTimerService.TimerState.IDLE) {
                    Button(
                        onClick = onSettingsClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Settings")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp)) // Add some space after the button
                }
                
                // Control buttons
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp), // Add some horizontal padding
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Button (visible when idle)
                        if (timerState == StudyTimerService.TimerState.IDLE) {
                            Button(
                                onClick = onStartClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50), // Green
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start")
                                }
                            }
                        }
                        
                        // Stop Button (visible when running)
                        if (timerState != StudyTimerService.TimerState.IDLE) {
                            Button(
                                onClick = onStopClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336), // Red
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Close, contentDescription = "Stop")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to format time in milliseconds to HH:MM:SS or MM:SS format.
 */
fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    
    return if (hours > 0) {
        String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }
}

@Preview(showBackground = true)
@Composable
fun TimerScreenPreviewIdle() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.IDLE,
            timeLeftInSession = 90 * 60 * 1000L,
            timeUntilNextAlarm = 0L,
            showNextAlarmTime = true,
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            breakDurationMin = 20,
            onStartClick = {}, 
            onStopClick = {}, 
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimerScreenPreviewStudying() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.STUDYING,
            timeLeftInSession = 45 * 60 * 1000L, 
            timeUntilNextAlarm = 2 * 60 * 1000L,
            showNextAlarmTime = true,
            studyDurationMin = 60, 
            minAlarmIntervalMin = 4, 
            maxAlarmIntervalMin = 6, 
            breakDurationMin = 13, 
            onStartClick = {}, 
            onStopClick = {}, 
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimerScreenPreviewBreak() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.BREAK,
            timeLeftInSession = 15 * 60 * 1000L,
            timeUntilNextAlarm = 0L,
            showNextAlarmTime = true,
            studyDurationMin = 90, 
            minAlarmIntervalMin = 3, 
            maxAlarmIntervalMin = 5, 
            breakDurationMin = 20, 
            onStartClick = {}, 
            onStopClick = {}, 
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimerScreenPreviewEyeRest() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.EYE_REST,
            timeLeftInSession = 5 * 1000L, // Show remaining eye rest time
            timeUntilNextAlarm = 0L,
            showNextAlarmTime = true,
            studyDurationMin = 90, 
            minAlarmIntervalMin = 3, 
            maxAlarmIntervalMin = 5, 
            breakDurationMin = 20, 
            onStartClick = {}, 
            onStopClick = {}, 
            onSettingsClick = {}
        )
    }
}
