package com.oymyisme.studytimer.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import com.oymyisme.studytimer.R
import com.oymyisme.studytimer.model.SoundOptions
import java.util.concurrent.TimeUnit
import com.oymyisme.studytimer.ui.theme.StudyTimerTheme
import java.util.Locale
import com.oymyisme.studytimer.model.TestMode

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
    
    // 调用更新的TimerDisplay组件，直接传递数据类对象
    TimerDisplay(
        timerState = timerState,
        settings = settings,
        studyDurationMin = settings.studyDurationMin,
        breakDurationMin = breakDurationMin,
        isTestModeActive = isTestModeActive
    )
}

/**
 * 计时器显示组件，负责显示倒计时和进度
 * 使用数据类版本的TimerState和TimerSettings，遵循高内聚、低耦合的设计原则
 */
@Composable
private fun TimerDisplay(
    timerState: com.oymyisme.model.TimerState,
    settings: com.oymyisme.model.TimerSettings,
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
        val progress = calculateProgressWithDataClasses(
            timerState = timerState,
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
            val totalCycleTimeLeft = calculateTotalCycleTimeLeftWithDataClasses(
                timerState = timerState,
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
            if (!timerState.isIdle) {
                val timerTextRes = when {
                    timerState.isStudying -> R.string.timer_study
                    timerState.isBreak -> R.string.timer_break
                    timerState.isEyeRest -> R.string.timer_eye_rest
                    else -> null
                }

                timerTextRes?.let {
                    Text(
                        text = stringResource(it, formatTime(timerState.timeLeftInSession)),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // 有条件地显示下一次闹钟时间
            if (timerState.isStudying && settings.showNextAlarmTime) {
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
                        text = " Next: ${formatTime(timerState.timeUntilNextAlarm)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 计算循环进度
 * 使用数据类进行计算，遵循高内聚、低耦合的设计原则
 */
private fun calculateProgressWithDataClasses(
    timerState: com.oymyisme.model.TimerState,
    totalCycleDurationMs: Long
): Float {
    // 使用数据类的属性和方法，遵循高内聚、低耦合的设计原则
    return if (timerState.isIdle) {
        0f // 空闲状态显示空圈
    } else {
        // 在所有非空闲状态下使用相同的进度计算
        if (totalCycleDurationMs > 0) {
            (timerState.elapsedTimeInFullCycle.toFloat() / totalCycleDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            1f // 避免除以0，如果总时长为0则显示完整循环
        }
    }
}

/**
 * 计算总周期剩余时间
 * 使用数据类进行计算，遵循高内聚、低耦合的设计原则
 */
private fun calculateTotalCycleTimeLeftWithDataClasses(
    timerState: com.oymyisme.model.TimerState,
    totalCycleDurationMs: Long,
    breakDurationMin: Int,
    studyDurationMin: Int,
    isTestModeActive: Boolean
): Long {
    // 使用数据类的属性和方法，遵循高内聚、低耦合的设计原则
    return when {
        timerState.isIdle -> totalCycleDurationMs
        
        timerState.isStudying -> {
            // 学习阶段，总周期倒计时 = 当前学习阶段剩余时间 + 休息时间
            timerState.timeLeftInSession + (if (isTestModeActive) TestMode.TEST_BREAK_TIME_MS else breakDurationMin * 60 * 1000L)
        }
        
        timerState.isBreak -> {
            // 休息阶段，总周期倒计时 = 当前休息阶段剩余时间
            timerState.timeLeftInSession
        }
        
        timerState.isEyeRest -> {
            // 眼部休息阶段，总周期倒计时不受影响
            val studyDurationMs = if (isTestModeActive) TestMode.TEST_STUDY_TIME_MS else studyDurationMin * 60 * 1000L
            
            if (timerState.elapsedTimeInFullCycle < studyDurationMs) {
                // 在学习阶段的眼部休息
                totalCycleDurationMs - timerState.elapsedTimeInFullCycle
            } else {
                // 在休息阶段的眼部休息
                totalCycleDurationMs - timerState.elapsedTimeInFullCycle
            }
        }
        
        else -> totalCycleDurationMs // 默认情况，避免程序异常
    }
}

/**
 * 控制按钮组件，包含设置和开始/停止按钮
 * 使用数据类版本的TimerState，遵循高内聚、低耦合的设计原则
 */
@Composable
private fun ControlButtons(
    timerState: com.oymyisme.model.TimerState,
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
            if (timerState.isIdle) {
                SettingsButton(onClick = onSettingsClick)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Start/Stop Button
            StartStopButton(
                isIdle = timerState.isIdle,
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
 * 使用数据类来简化参数传递，提高代码的可维护性
 * 
 * @param timerState 计时器状态数据类，包含所有状态信息
 * @param settings 计时器设置数据类，包含所有配置信息
 * @param testModeChangeTrigger 测试模式变化触发器，用于强制UI重组
 * @param onStartClick 开始按钮点击回调
 * @param onStopClick 停止按钮点击回调
 * @param onSettingsClick 设置按钮点击回调
 * @param onTestModeToggle 测试模式切换回调
 * @param onContinueNextCycle 继续下一个周期回调
 * @param onReturnToMain 返回主界面回调
 */
@Composable
fun StudyTimerApp(
    timerState: com.oymyisme.model.TimerState,
    settings: com.oymyisme.model.TimerSettings,
    testModeChangeTrigger: String = "", // 测试模式变化触发器，用于强制UI重组
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTestModeToggle: (Boolean) -> Unit = {},
    onContinueNextCycle: () -> Unit = {},
    onReturnToMain: () -> Unit = {}
) {
    // 直接使用数据类调用内部实现
    StudyTimerAppImpl(
        timerState = timerState,
        settings = settings,
        testModeChangeTrigger = testModeChangeTrigger,
        onStartClick = onStartClick,
        onStopClick = onStopClick,
        onSettingsClick = onSettingsClick,
        onTestModeToggle = onTestModeToggle,
        onContinueNextCycle = onContinueNextCycle,
        onReturnToMain = onReturnToMain
    )
}

// 已删除废弃的兴容性函数，统一使用数据类版本

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
                    // 状态文本显示 - 使用数据类的属性和方法，遵循高内聚、低耦合的设计原则
                    val statusText = when {
                        timerState.isStudying -> stringResource(
                            R.string.state_studying,
                            settings.minAlarmIntervalMin,
                            settings.maxAlarmIntervalMin
                        )
                        timerState.isBreak -> stringResource(R.string.state_break)
                        timerState.isEyeRest -> stringResource(R.string.state_eye_rest)
                        timerState.isIdle -> if (isTestModeActive) {
                            stringResource(
                                R.string.state_idle_test,
                                TestMode.TEST_STUDY_TIME_MS / 1000, // 直接使用毫秒常量转换为秒
                                TestMode.TEST_BREAK_TIME_MS / 1000,
                                TestMode.TEST_ALARM_INTERVAL_MS / 1000,
                                TestMode.TEST_ALARM_INTERVAL_MS / 1000 // 测试模式下最小和最大闹钟间隔相同
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
                        else -> "" // 默认情况，避免程序异常
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
                    if (timerState.isIdle) {
                        TimerTestModeSwitch(
                            testModeEnabled = settings.testModeEnabled,
                            onTestModeToggle = onTestModeToggle
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 控制按钮
                    ControlButtons(
                        timerState = timerState,
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
            // 使用数据类版本的StudyTimerApp，遵循高内聚、低耦合的设计原则
            // 使用工厂方法创建TimerState对象，避免直接引用TimerManager.TimerState枚举
            val timerState = com.oymyisme.model.TimerState.idle(
                timeLeftInSession = 90 * 60 * 1000L,
                elapsedTimeInFullCycle = 110 * 60 * 1000L, // For IDLE, typically means full cycle completed or ready
                cycleCompleted = false
            )
            
            val settings = com.oymyisme.model.TimerSettings(
                studyDurationMin = 90,
                minAlarmIntervalMin = 3,
                maxAlarmIntervalMin = 5,
                showNextAlarmTime = false,
                alarmSoundType = SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
                eyeRestSoundType = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
                testModeEnabled = false,
                timeUnit = com.oymyisme.model.TimeUnit.MINUTES
            )
            
            StudyTimerApp(
                timerState = timerState,
                settings = settings,
                testModeChangeTrigger = "",
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
            // 使用数据类版本的StudyTimerApp，遵循高内聚、低耦合的设计原则
            // 使用工厂方法创建TimerState对象，避免直接引用TimerManager.TimerState枚举
            val timerState = com.oymyisme.model.TimerState.studying(
                timeLeftInSession = 45 * 60 * 1000L, // Halfway through 90 min study
                timeUntilNextAlarm = 2 * 60 * 1000L,
                elapsedTimeInFullCycle = 45 * 60 * 1000L // Halfway through study part of full cycle
            )
            
            val settings = com.oymyisme.model.TimerSettings(
                studyDurationMin = 90,
                minAlarmIntervalMin = 3,
                maxAlarmIntervalMin = 5,
                showNextAlarmTime = true,
                alarmSoundType = SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
                eyeRestSoundType = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
                testModeEnabled = false,
                timeUnit = com.oymyisme.model.TimeUnit.MINUTES
            )
            
            StudyTimerApp(
                timerState = timerState,
                settings = settings,
                testModeChangeTrigger = "",
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
            // 使用数据类版本的StudyTimerApp，遵循高内聚、低耦合的设计原则
            // 使用工厂方法创建TimerState对象，避免直接引用TimerManager.TimerState枚举
            val timerState = com.oymyisme.model.TimerState.breakState(
                timeLeftInSession = 10 * 60 * 1000L, // Halfway through 20 min break
                elapsedTimeInFullCycle = (90 * 60 * 1000L) + (10 * 60 * 1000L) // Study done + halfway through break
            )
            
            val settings = com.oymyisme.model.TimerSettings(
                studyDurationMin = 90,
                minAlarmIntervalMin = 3,
                maxAlarmIntervalMin = 5,
                showNextAlarmTime = false,
                alarmSoundType = SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
                eyeRestSoundType = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
                testModeEnabled = false,
                timeUnit = com.oymyisme.model.TimeUnit.MINUTES
            )
            
            StudyTimerApp(
                timerState = timerState,
                settings = settings,
                testModeChangeTrigger = "",
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
        // 使用数据类版本的StudyTimerApp，遵循高内聚、低耦合的设计原则
        // 使用工厂方法创建TimerState对象，避免直接引用TimerManager.TimerState枚举
        val timerState = com.oymyisme.model.TimerState.eyeRest(
            timeLeftInSession = 5 * 1000L, // Halfway through 10s eye rest
            elapsedTimeInFullCycle = 30 * 60 * 1000L // Example: eye rest during a study session
        )
        
        val settings = com.oymyisme.model.TimerSettings(
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            showNextAlarmTime = false,
            alarmSoundType = SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
            eyeRestSoundType = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
            testModeEnabled = false,
            timeUnit = com.oymyisme.model.TimeUnit.MINUTES
        )
        
        StudyTimerApp(
            timerState = timerState,
            settings = settings,
            testModeChangeTrigger = "",
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
