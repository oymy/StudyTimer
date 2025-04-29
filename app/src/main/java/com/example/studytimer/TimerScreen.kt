package com.example.studytimer

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.TimeUnit

/**
 * The main UI composable for the Timer screen.
 */
@Composable
fun StudyTimerApp(
    timerState: StudyTimerService.TimerState,
    timeLeftInSession: Long,
    timeUntilNextAlarm: Long,
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
                        progress = if (timerState == StudyTimerService.TimerState.STUDYING || timerState == StudyTimerService.TimerState.BREAK) {
                            (timeLeftInSession / (90f * 60 * 1000)).toFloat() // Adjust total time if needed
                        } else {
                            1f // Full circle when idle or resting
                        },
                        modifier = Modifier.size(200.dp),
                        strokeWidth = 12.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer
                    )
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
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
