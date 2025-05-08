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
import androidx.compose.ui.res.stringResource
import java.util.concurrent.TimeUnit
import com.oymyisme.studytimer.timer.TimerManager
import com.oymyisme.studytimer.ui.theme.StudyTimerTheme
import java.util.Locale

/**
 * The main UI composable for the Timer screen.
 */
@Composable
fun StudyTimerApp(
    timerState: TimerManager.TimerState,
    timeLeftInSession: Long,
    timeUntilNextAlarm: Long,
    elapsedTimeInFullCycle: Long,
    showNextAlarmTime: Boolean,
    studyDurationMin: Int,
    minAlarmIntervalMin: Int,
    maxAlarmIntervalMin: Int,
    breakDurationMin: Int,
    testModeEnabled: Boolean = false,
    testModeChangeTrigger: String = "", // 添加测试模式变化触发器参数，用于强制重组
    cycleCompleted: Boolean = false,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTestModeToggle: (Boolean) -> Unit = {},
    onContinueNextCycle: () -> Unit = {},
    onReturnToMain: () -> Unit = {}
) {
    // 定义一个变量来存储是否在测试模式下，避免重复检查
    val isTestModeActive = testModeEnabled && TestMode.isEnabled
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
                    text = stringResource(R.string.app_name),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Timer Display (CircularProgressIndicator + Text)
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = {
                            
                            val currentTotalCycleDurationMillis = if (isTestModeActive) {
                                // 测试模式下的总周期时长
                                TestMode.TEST_STUDY_TIME_MS + TestMode.TEST_BREAK_TIME_MS
                            }
                            else {
                                // 非测试模式下的总周期时长
                                studyDurationMin * 60 * 1000L + breakDurationMin * 60 * 1000L
                            }

                            when (timerState) {
                                TimerManager.TimerState.STUDYING,
                                TimerManager.TimerState.BREAK,
                                TimerManager.TimerState.EYE_REST -> {
                                    // 修改：在眼部休息期间也使用相同的进度计算
                                    if (currentTotalCycleDurationMillis > 0) {
                                        (elapsedTimeInFullCycle.toFloat() / currentTotalCycleDurationMillis.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        1f // Avoid division by zero, show full if total is somehow zero
                                    }
                                }
                                TimerManager.TimerState.IDLE -> 0f // Empty circle when idle, consistent with reset progress
                            }
                        }(),
                        modifier = Modifier.size(300.dp), // 进一步增大圈圈进度条大小
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 20.dp, // 进一步增加描边宽度
                        trackColor = MaterialTheme.colorScheme.secondaryContainer,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 计算总周期时长（学习+休息）
                        val totalCycleDurationMs = if (isTestModeActive) {
                            // 测试模式下的总周期时长
                            TestMode.TEST_STUDY_TIME_MS + TestMode.TEST_BREAK_TIME_MS
                        } else {
                            // 非测试模式下的总周期时长
                            studyDurationMin * 60 * 1000L + breakDurationMin * 60 * 1000L
                        }
                        
                        // 计算并显示总周期的倒计时
                        val totalCycleTimeLeft = when (timerState) {
                            TimerManager.TimerState.IDLE -> totalCycleDurationMs
                            TimerManager.TimerState.STUDYING -> {
                                // 学习阶段，总周期倒计时 = 当前学习阶段剩余时间 + 休息时间
                                timeLeftInSession + (if (isTestModeActive) TestMode.TEST_BREAK_TIME_MS else breakDurationMin * 60 * 1000L)
                            }
                            TimerManager.TimerState.BREAK -> {
                                // 休息阶段，总周期倒计时 = 当前休息阶段剩余时间
                                timeLeftInSession
                            }
                            TimerManager.TimerState.EYE_REST -> {
                                // 眼部休息阶段，总周期倒计时不受影响
                                if (elapsedTimeInFullCycle < (if (isTestModeActive) TestMode.TEST_STUDY_TIME_MS else studyDurationMin * 60 * 1000L)) {
                                    // 在学习阶段的眼部休息
                                    totalCycleDurationMs - elapsedTimeInFullCycle
                                } else {
                                    // 在休息阶段的眼部休息
                                    (totalCycleDurationMs - elapsedTimeInFullCycle).coerceAtLeast(0L)
                                }
                            }
                        }
                        
                        // 显示总周期倒计时
                        Text(
                            text = stringResource(R.string.timer_cycle, formatTime(totalCycleTimeLeft)),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 显示当前阶段倒计时
                        if (timerState != TimerManager.TimerState.IDLE) {
                            val timerTextRes = when (timerState) {
                                TimerManager.TimerState.STUDYING -> R.string.timer_study
                                TimerManager.TimerState.BREAK -> R.string.timer_break
                                TimerManager.TimerState.EYE_REST -> R.string.timer_eye_rest
                                else -> null
                            }
                            
                            timerTextRes?.let {
                                Text(
                                    text = stringResource(it, formatTime(timeLeftInSession)),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Conditionally show the next alarm time row
                        if (timerState == TimerManager.TimerState.STUDYING && showNextAlarmTime) {
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
                        TimerManager.TimerState.STUDYING ->
                            stringResource(R.string.state_studying, minAlarmIntervalMin, maxAlarmIntervalMin)

                        TimerManager.TimerState.BREAK ->
                            stringResource(R.string.state_break)

                        TimerManager.TimerState.EYE_REST ->
                            stringResource(R.string.state_eye_rest)

                        TimerManager.TimerState.IDLE -> {
                            if (isTestModeActive) {
                                // 测试模式下显示测试时间
                                stringResource(
                                    R.string.state_idle_test,
                                    TestMode.getStudyDurationSec(),
                                    TestMode.getBreakDurationSec(),
                                    TestMode.getMinAlarmIntervalSec(),
                                    TestMode.getMaxAlarmIntervalSec()
                                )
                            } else {
                                // 非测试模式下显示实际设置的学习周期
                                stringResource(
                                    R.string.state_idle_normal,
                                    studyDurationMin,
                                    breakDurationMin,
                                    minAlarmIntervalMin,
                                    maxAlarmIntervalMin
                                )
                            }
                        }
                    },
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 测试模式开关 - 始终显示，但只在空闲状态下显示
                if (timerState == TimerManager.TimerState.IDLE) {
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
                                    text = stringResource(R.string.test_mode),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.test_mode_description),
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
                        if (timerState == TimerManager.TimerState.IDLE) {
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
                                        contentDescription = stringResource(R.string.settings),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.settings))
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
                                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.start))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.start))
                                }
                            }
                        }

                        // Stop Button (visible when running)
                        if (timerState != TimerManager.TimerState.IDLE) {
                            Button(
                                onClick = onStopClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336), // Red
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.stop))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.stop))
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
            timerState = TimerManager.TimerState.IDLE,
            timeLeftInSession = 90 * 60 * 1000L,
            timeUntilNextAlarm = 0L,
            elapsedTimeInFullCycle = 110 * 60 * 1000L, // For IDLE, typically means full cycle completed or ready
            showNextAlarmTime = false,
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            breakDurationMin = 20,
            testModeEnabled = false,
            testModeChangeTrigger = "",
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
            timerState = TimerManager.TimerState.STUDYING,
            timeLeftInSession = 45 * 60 * 1000L, // Halfway through 90 min study
            timeUntilNextAlarm = 2 * 60 * 1000L,
            elapsedTimeInFullCycle = 45 * 60 * 1000L, // Halfway through study part of full cycle
            showNextAlarmTime = true,
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            breakDurationMin = 20,
            testModeEnabled = false,
            testModeChangeTrigger = "",
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
            timerState = TimerManager.TimerState.BREAK,
            timeLeftInSession = 10 * 60 * 1000L, // Halfway through 20 min break
            timeUntilNextAlarm = 0L,
            elapsedTimeInFullCycle = (90 * 60 * 1000L) + (10 * 60 * 1000L), // Study done + halfway through break
            showNextAlarmTime = false,
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            breakDurationMin = 20,
            testModeEnabled = false,
            testModeChangeTrigger = "",
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
            timerState = TimerManager.TimerState.EYE_REST,
            timeLeftInSession = 5 * 1000L, // Halfway through 10s eye rest
            timeUntilNextAlarm = 0L,
            elapsedTimeInFullCycle = 30 * 60 * 1000L, // Example: eye rest during a study session
            showNextAlarmTime = false,
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            breakDurationMin = 20,
            testModeEnabled = false,
            testModeChangeTrigger = "",
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
        onDismissRequest = { /* 不允许通过点击外部关闭 */ },
        title = { Text(stringResource(R.string.cycle_completed_title)) },
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
                    text = stringResource(R.string.cycle_completed_question),
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
                    Text(stringResource(R.string.continue_next_cycle))
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
                    Text(stringResource(R.string.return_to_main))
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
