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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.oymyisme.studytimer.BuildConfig

/**
 * 时间格式化工具函数
 */
private fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return if (hours > 0) {
        String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }
}

/**
 * 计时器显示组件，负责显示倒计时和进度
 * 使用新的数据结构来减少参数传递
 * 遵循高内聚、低耦合的设计原则
 */
@Composable
private fun TimerDisplayWithDataClasses(
    timerState: com.oymyisme.model.TimerState,
    settings: com.oymyisme.model.TimerSettings,
    breakDurationMin: Int,
    isTestModeActive: Boolean
) {
    // 计算总周期时长（学习+休息）
    val totalCycleDurationMs = if (isTestModeActive) {
        // 测试模式下的总周期时长
        TestMode.TEST_STUDY_TIME_MS + TestMode.TEST_BREAK_TIME_MS
    } else {
        // 非测试模式下的总周期时长
        settings.studyDurationMin * 60 * 1000L + breakDurationMin * 60 * 1000L
    }
    
    // 使用新的辅助函数计算进度
    val progress = calculateProgressWithDataClasses(
        timerState = timerState,
        totalCycleDurationMs = totalCycleDurationMs
    )
    
    // 调用原有的TimerDisplay组件，传递解析后的参数
    TimerDisplay(
        timerState = timerState.timerState,
        timeLeftInSession = timerState.timeLeftInSession,
        timeUntilNextAlarm = timerState.timeUntilNextAlarm,
        elapsedTimeInFullCycle = timerState.elapsedTimeInFullCycle,
        showNextAlarmTime = settings.showNextAlarmTime,
        studyDurationMin = settings.studyDurationMin,
        breakDurationMin = breakDurationMin,
        isTestModeActive = isTestModeActive
    )
}

/**
 * 计时器显示组件，负责显示倒计时和进度
 */
@Composable
private fun TimerDisplay(
    timerState: TimerManager.TimerState,
    timeLeftInSession: Long,
    timeUntilNextAlarm: Long,
    elapsedTimeInFullCycle: Long,
    showNextAlarmTime: Boolean,
    studyDurationMin: Int,
    breakDurationMin: Int,
    isTestModeActive: Boolean
) {
    // 使用固定大小的Box来确保计时器部分的位置固定
    Box(
        modifier = Modifier
            .height(320.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // 计算总周期时长（学习+休息）
        val totalCycleDurationMs = if (isTestModeActive) {
            // 测试模式下的总周期时长
            TestMode.TEST_STUDY_TIME_MS + TestMode.TEST_BREAK_TIME_MS
        } else {
            // 非测试模式下的总周期时长
            studyDurationMin * 60 * 1000L + breakDurationMin * 60 * 1000L
        }
        
        // 计算循环进度
        val progress = calculateProgress(
            timerState = timerState,
            elapsedTimeInFullCycle = elapsedTimeInFullCycle,
            totalCycleDurationMs = totalCycleDurationMs
        )
        
        // 显示循环进度指示器
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.size(300.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 20.dp,
            trackColor = MaterialTheme.colorScheme.secondaryContainer,
        )
        
        // 显示时间文本
        Column(
            modifier = Modifier.height(160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 计算并显示总周期的倒计时
            val totalCycleTimeLeft = calculateTotalCycleTimeLeft(
                timerState = timerState,
                timeLeftInSession = timeLeftInSession,
                elapsedTimeInFullCycle = elapsedTimeInFullCycle,
                totalCycleDurationMs = totalCycleDurationMs,
                breakDurationMin = breakDurationMin,
                studyDurationMin = studyDurationMin,
                isTestModeActive = isTestModeActive
            )

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

            // 有条件地显示下一次闹钟时间
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
}

/**
 * 计算循环进度 - 使用新的数据结构
 */
private fun calculateProgressWithDataClasses(
    timerState: com.oymyisme.model.TimerState,
    totalCycleDurationMs: Long
): Float {
    return calculateProgress(
        timerState = timerState.timerState,
        elapsedTimeInFullCycle = timerState.elapsedTimeInFullCycle,
        totalCycleDurationMs = totalCycleDurationMs
    )
}

/**
 * 计算循环进度
 */
private fun calculateProgress(
    timerState: TimerManager.TimerState,
    elapsedTimeInFullCycle: Long,
    totalCycleDurationMs: Long
): Float {
    return when (timerState) {
        TimerManager.TimerState.STUDYING,
        TimerManager.TimerState.BREAK,
        TimerManager.TimerState.EYE_REST -> {
            // 在所有非空闲状态下使用相同的进度计算
            if (totalCycleDurationMs > 0) {
                (elapsedTimeInFullCycle.toFloat() / totalCycleDurationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                1f // 避免除以0，如果总时长为0则显示完整循环
            }
        }
        TimerManager.TimerState.IDLE -> 0f // 空闲状态显示空圈
    }
}

/**
 * 计算总周期剩余时间 - 使用新的数据结构
 */
private fun calculateTotalCycleTimeLeftWithDataClasses(
    timerState: com.oymyisme.model.TimerState,
    totalCycleDurationMs: Long,
    breakDurationMin: Int,
    studyDurationMin: Int,
    isTestModeActive: Boolean
): Long {
    return calculateTotalCycleTimeLeft(
        timerState = timerState.timerState,
        timeLeftInSession = timerState.timeLeftInSession,
        elapsedTimeInFullCycle = timerState.elapsedTimeInFullCycle,
        totalCycleDurationMs = totalCycleDurationMs,
        breakDurationMin = breakDurationMin,
        studyDurationMin = studyDurationMin,
        isTestModeActive = isTestModeActive
    )
}

/**
 * 计算总周期剩余时间
 */
private fun calculateTotalCycleTimeLeft(
    timerState: TimerManager.TimerState,
    timeLeftInSession: Long,
    elapsedTimeInFullCycle: Long,
    totalCycleDurationMs: Long,
    breakDurationMin: Int,
    studyDurationMin: Int,
    isTestModeActive: Boolean
): Long {
    return when (timerState) {
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
            val studyDurationMs = if (isTestModeActive) TestMode.TEST_STUDY_TIME_MS else studyDurationMin * 60 * 1000L
            
            if (elapsedTimeInFullCycle < studyDurationMs) {
                // 在学习阶段的眼部休息
                totalCycleDurationMs - elapsedTimeInFullCycle
            } else {
                // 在休息阶段的眼部休息
                (totalCycleDurationMs - elapsedTimeInFullCycle).coerceAtLeast(0L)
            }
        }
    }
}

/**
 * 控制按钮组件，包含设置和开始/停止按钮
 */
@Composable
private fun ControlButtons(
    timerState: TimerManager.TimerState,
    onSettingsClick: () -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
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
            // Settings Button (只在空闲状态显示)
            if (timerState == TimerManager.TimerState.IDLE) {
                SettingsButton(onClick = onSettingsClick)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Start/Stop Button
            StartStopButton(
                isIdle = timerState == TimerManager.TimerState.IDLE,
                onStartClick = onStartClick,
                onStopClick = onStopClick
            )
        }
    }
}

/**
 * 设置按钮组件
 */
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
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
}

/**
 * 开始/停止按钮组件
 */
@Composable
private fun StartStopButton(
    isIdle: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val buttonColor = if (isIdle) Color(0xFF4CAF50) else Color(0xFFF44336) // Green or Red
    val icon = if (isIdle) Icons.Default.PlayArrow else Icons.Default.Close
    val text = stringResource(if (isIdle) R.string.start else R.string.stop)
    val contentDescription = if (isIdle) stringResource(R.string.start) else stringResource(R.string.stop)
    val onClick = if (isIdle) onStartClick else onStopClick
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text)
        }
    }
}

/**
 * 测试模式开关组件 - 计时器屏幕版本
 */
@Composable
private fun TimerTestModeSwitch(
    testModeEnabled: Boolean,
    onTestModeToggle: (Boolean) -> Unit
) {
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
            
            // 使用状态提升模式，避免在组件内部维护状态
            Switch(
                checked = testModeEnabled,
                onCheckedChange = onTestModeToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

/**
 * The main UI composable for the Timer screen.
 * 使用新的数据结构来简化参数传递
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
)
{
    // 定义一个变量来存储是否在测试模式下，避免重复检查
    val isTestModeActive = testModeEnabled && TestMode.isEnabled
    
    // 将原始参数映射到新的数据结构
    val currentTimerState = com.oymyisme.model.TimerState(
        timerState = timerState,
        timeLeftInSession = timeLeftInSession,
        timeUntilNextAlarm = timeUntilNextAlarm,
        elapsedTimeInFullCycle = elapsedTimeInFullCycle,
        cycleCompleted = cycleCompleted
    )
    
    val currentSettings = com.oymyisme.model.TimerSettings(
        studyDurationMin = studyDurationMin,
        minAlarmIntervalMin = minAlarmIntervalMin,
        maxAlarmIntervalMin = maxAlarmIntervalMin,
        showNextAlarmTime = showNextAlarmTime,
        testModeEnabled = testModeEnabled,
        // 使用默认值
        alarmSoundType = SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
        eyeRestSoundType = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE
    )
    
    // 使用新的数据结构调用内部实现
    StudyTimerAppImpl(
        timerState = currentTimerState,
        settings = currentSettings,
        testModeChangeTrigger = testModeChangeTrigger,
        onStartClick = onStartClick,
        onStopClick = onStopClick,
        onSettingsClick = onSettingsClick,
        onTestModeToggle = onTestModeToggle,
        onContinueNextCycle = onContinueNextCycle,
        onReturnToMain = onReturnToMain
    )
}

/**
 * StudyTimerApp的内部实现，使用新的数据结构
 */
@Composable
private fun StudyTimerAppImpl(
    timerState: com.oymyisme.model.TimerState,
    settings: com.oymyisme.model.TimerSettings,
    testModeChangeTrigger: String = "",
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTestModeToggle: (Boolean) -> Unit = {},
    onContinueNextCycle: () -> Unit = {},
    onReturnToMain: () -> Unit = {}
) {
    // 定义一个变量来存储是否在测试模式下，避免重复检查
    val isTestModeActive = settings.testModeEnabled && TestMode.isEnabled
    
    // 周期完成对话框
    if (timerState.cycleCompleted) {
        Log.d("TimerScreen", "Showing cycle completed dialog, cycleCompleted=${timerState.cycleCompleted}")
        Box(modifier = Modifier.fillMaxSize()) {
            CycleCompletedDialog(
                onContinueNextCycle = onContinueNextCycle,
                onReturnToMain = onReturnToMain
            )
        }
    }

    // 使用全局格式化时间函数，避免重复定义

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
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 添加可滚动的Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // App title
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 移除多余的Spacer，因为我们已经使用Arrangement.spacedBy来控制间距

                    // Timer Display (CircularProgressIndicator + Text)
                    TimerDisplayWithDataClasses(
                        timerState = timerState,
                        settings = settings,
                        breakDurationMin = settings.breakDurationMin,
                        isTestModeActive = isTestModeActive
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instructions
                    // 状态文本显示
                    val statusText = when (timerState.timerState) {
                        TimerManager.TimerState.STUDYING -> stringResource(
                            R.string.state_studying,
                            settings.minAlarmIntervalMin,
                            settings.maxAlarmIntervalMin
                        )
                        TimerManager.TimerState.BREAK -> stringResource(R.string.state_break)
                        TimerManager.TimerState.EYE_REST -> stringResource(R.string.state_eye_rest)
                        TimerManager.TimerState.IDLE -> if (isTestModeActive) {
                            stringResource(
                                R.string.state_idle_test,
                                TestMode.getStudyDurationSec(),
                                TestMode.getBreakDurationSec(),
                                TestMode.getMinAlarmIntervalSec(),
                                TestMode.getMaxAlarmIntervalSec()
                            )
                        } else {
                            stringResource(
                                R.string.state_idle_normal,
                                settings.studyDurationMin,
                                settings.breakDurationMin,
                                settings.minAlarmIntervalMin,
                                settings.maxAlarmIntervalMin
                            )
                        }
                    }
                    Text(
                        text = statusText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 测试模式开关 - 仅在空闲状态下显示
                    if (timerState.timerState == TimerManager.TimerState.IDLE) {
                        TimerTestModeSwitch(
                            testModeEnabled = settings.testModeEnabled,
                            onTestModeToggle = onTestModeToggle
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 控制按钮
                    ControlButtons(
                        timerState = timerState.timerState,
                        onSettingsClick = onSettingsClick,
                        onStartClick = onStartClick,
                        onStopClick = onStopClick
                    )
                }
            }
        }

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
                    text = stringResource(R.string.cycle_completed_message),
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
