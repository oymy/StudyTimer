package com.oymyisme.studytimer

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.DialogProperties
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
    elapsedTimeInFullCycle: Long,
    showNextAlarmTime: Boolean,
    studyDurationMin: Int,
    minAlarmIntervalMin: Int,
    maxAlarmIntervalMin: Int,
    breakDurationMin: Int,
    testModeEnabled: Boolean = false,
    cycleCompleted: Boolean = false,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTestModeToggle: (Boolean) -> Unit = {},
    onContinueNextCycle: () -> Unit = {},
    onReturnToMain: () -> Unit = {}
) {
    // 周期完成对话框
    if (cycleCompleted) {
        Log.d("TimerScreen", "Showing cycle completed dialog, cycleCompleted=$cycleCompleted")
        Box(modifier = Modifier.fillMaxSize()) {
            CycleCompletedDialog(
                onContinueNextCycle = onContinueNextCycle,
                onReturnToMain = onReturnToMain
            )
        }
    }
    
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
                        progress = {
                            val currentTotalCycleDurationMillis = when {
                                testModeEnabled && TestMode.isEnabled -> {
                                    // 测试模式下的总周期时长：30秒学习 + 10秒休息 = 40秒
                                    // 直接使用确切的毫秒值，而不是通过常量计算，避免精度问题
                                    30 * 1000L + 10 * 1000L // 30秒学习 + 10秒休息
                                }
                                else -> {
                                    studyDurationMin * 60 * 1000L + breakDurationMin * 60 * 1000L
                                }
                            }

                            when (timerState) {
                                StudyTimerService.TimerState.STUDYING,
                                StudyTimerService.TimerState.BREAK,
                                StudyTimerService.TimerState.EYE_REST -> { // 修改：在眼部休息期间也使用相同的进度计算
                                    if (currentTotalCycleDurationMillis > 0) {
                                        (elapsedTimeInFullCycle.toFloat() / currentTotalCycleDurationMillis.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        1f // Avoid division by zero, show full if total is somehow zero
                                    }
                                }
                                StudyTimerService.TimerState.IDLE -> 1f // Full circle when idle
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

                // 测试模式开关 - 只在调试版本中显示
                if (timerState == StudyTimerService.TimerState.IDLE && TestMode.isEnabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (testModeEnabled)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "测试模式",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "学习：30秒 闹钟：10秒 休息：10秒",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // 使用 remember 记录当前开关状态，并从外部状态更新
                            var switchState by remember { mutableStateOf(testModeEnabled) }
                            // 当外部状态变化时更新开关状态
                            LaunchedEffect(testModeEnabled) {
                                switchState = testModeEnabled
                            }

                            Switch(
                                checked = switchState,
                                onCheckedChange = { newState ->
                                    // 先更新本地状态，然后通知外部
                                    switchState = newState
                                    onTestModeToggle(newState)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 当计时器空闲时显示 Settings 和 Start 按钮
                        if (timerState == StudyTimerService.TimerState.IDLE) {
                            // Settings Button
                            Button(
                                onClick = onSettingsClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF009688), // 蓝绿色，与绿色相配
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Settings")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Start Button
                            Button(
                                onClick = onStartClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50), // Green
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
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


@Preview(showBackground = true, name = "Timer Screen - Idle")
@Composable
fun TimerScreenPreviewIdle() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.IDLE,
            timeLeftInSession = 90 * 60 * 1000L,
            timeUntilNextAlarm = 0L,
            elapsedTimeInFullCycle = 110 * 60 * 1000L, // For IDLE, typically means full cycle completed or ready
            showNextAlarmTime = false,
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            breakDurationMin = 20,
            testModeEnabled = false,
            cycleCompleted = false,
            onStartClick = {}, 
            onStopClick = {}, 
            onSettingsClick = {},
            onTestModeToggle = {},
            onContinueNextCycle = {},
            onReturnToMain = {}
        )
    }
}

@Preview(showBackground = true, name = "Timer Screen - Studying")
@Composable
fun TimerScreenPreviewStudying() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.STUDYING,
            timeLeftInSession = 45 * 60 * 1000L, // Halfway through 90 min study
            timeUntilNextAlarm = 2 * 60 * 1000L,
            elapsedTimeInFullCycle = 45 * 60 * 1000L, // Halfway through study part of full cycle
            showNextAlarmTime = true,
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            breakDurationMin = 20,
            testModeEnabled = false,
            cycleCompleted = false,
            onStartClick = {}, 
            onStopClick = {}, 
            onSettingsClick = {},
            onTestModeToggle = {},
            onContinueNextCycle = {},
            onReturnToMain = {}
        )
    }
}

@Preview(showBackground = true, name = "Timer Screen - Break")
@Composable
fun TimerScreenPreviewBreak() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.BREAK,
            timeLeftInSession = 10 * 60 * 1000L, // Halfway through 20 min break
            timeUntilNextAlarm = 0L,
            elapsedTimeInFullCycle = (90 * 60 * 1000L) + (10 * 60 * 1000L), // Study done + halfway through break
            showNextAlarmTime = false,
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            breakDurationMin = 20,
            testModeEnabled = false,
            cycleCompleted = false,
            onStartClick = {}, 
            onStopClick = {}, 
            onSettingsClick = {},
            onTestModeToggle = {},
            onContinueNextCycle = {},
            onReturnToMain = {}
        )
    }
}

@Preview(showBackground = true, name = "Timer Screen - Eye Rest")
@Composable
fun TimerScreenPreviewEyeRest() {
    StudyTimerTheme {
        StudyTimerApp(
            timerState = StudyTimerService.TimerState.EYE_REST,
            timeLeftInSession = 5 * 1000L, // Halfway through 10s eye rest
            timeUntilNextAlarm = 0L,
            elapsedTimeInFullCycle = 30 * 60 * 1000L, // Example: eye rest during a study session
            showNextAlarmTime = false,
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            breakDurationMin = 20,
            testModeEnabled = false,
            cycleCompleted = false,
            onStartClick = {}, 
            onStopClick = {}, 
            onSettingsClick = {},
            onTestModeToggle = {},
            onContinueNextCycle = {},
            onReturnToMain = {}
        )
    }
}

@Preview(showBackground = true, name = "Cycle Completed Dialog")
@Composable
fun CycleCompletedDialogPreview() {
    StudyTimerTheme {
        CycleCompletedDialog(
            onContinueNextCycle = {},
            onReturnToMain = {}
        )
    }
}

/**
 * 周期完成对话框
 */
@Composable
fun CycleCompletedDialog(
    onContinueNextCycle: () -> Unit,
    onReturnToMain: () -> Unit
) {
    // 使用标准的 AlertDialog，确保在最上层显示
    AlertDialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        title = {
            Text(
                text = "恭喜完成学习周期！",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "你已经完成了一个完整的学习周期，包括学习和休息时间。",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "你想继续下一个周期还是返回主界面？",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onContinueNextCycle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(0.45f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = "Continue")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("继续学习")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReturnToMain,
                modifier = Modifier.fillMaxWidth(0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = "Return")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("返回")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(16.dp),
        // 添加遮罩背景色
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight()
    )
}
