package com.oymyisme.studytimer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oymyisme.studytimer.ui.theme.StudyTimerTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    studyDurationFlow: StateFlow<Int>,
    minAlarmIntervalFlow: StateFlow<Int>,
    maxAlarmIntervalFlow: StateFlow<Int>,
    showNextAlarmTimeFlow: StateFlow<Boolean>,
    alarmSoundTypeFlow: StateFlow<String>,
    eyeRestSoundTypeFlow: StateFlow<String>,
    onStudyDurationChange: (Int) -> Unit,
    onMinAlarmIntervalChange: (Int) -> Unit,
    onMaxAlarmIntervalChange: (Int) -> Unit,
    onShowNextAlarmTimeChange: (Boolean) -> Unit,
    onAlarmSoundTypeChange: (String) -> Unit,
    onEyeRestSoundTypeChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToThemeSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 添加滚动状态
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState) // 添加滚动功能
        ) {
            // 主题设置卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    // 使用主题的 surfaceVariant 颜色，自动适应暗色模式
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.appearance_settings),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 主题设置选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.theme_settings),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.theme_settings_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(onClick = onNavigateToThemeSettings) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "进入主题设置",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // 计时器设置卡片
            SettingsCard(
                studyDurationFlow = studyDurationFlow,
                minAlarmIntervalFlow = minAlarmIntervalFlow,
                maxAlarmIntervalFlow = maxAlarmIntervalFlow,
                showNextAlarmTimeFlow = showNextAlarmTimeFlow,
                onStudyDurationChange = onStudyDurationChange,
                onMinAlarmIntervalChange = onMinAlarmIntervalChange,
                onMaxAlarmIntervalChange = onMaxAlarmIntervalChange,
                onShowNextAlarmTimeChange = onShowNextAlarmTimeChange,
                alarmSoundTypeFlow = alarmSoundTypeFlow,
                eyeRestSoundTypeFlow = eyeRestSoundTypeFlow,
                onAlarmSoundTypeChange = onAlarmSoundTypeChange,
                onEyeRestSoundTypeChange = onEyeRestSoundTypeChange
            )
        }
    }
}

@Composable
fun SettingsCard(
    studyDurationFlow: StateFlow<Int>,
    minAlarmIntervalFlow: StateFlow<Int>,
    maxAlarmIntervalFlow: StateFlow<Int>,
    showNextAlarmTimeFlow: StateFlow<Boolean>,
    alarmSoundTypeFlow: StateFlow<String>,
    eyeRestSoundTypeFlow: StateFlow<String>,
    onStudyDurationChange: (Int) -> Unit,
    onMinAlarmIntervalChange: (Int) -> Unit,
    onMaxAlarmIntervalChange: (Int) -> Unit,
    onShowNextAlarmTimeChange: (Boolean) -> Unit,
    onAlarmSoundTypeChange: (String) -> Unit,
    onEyeRestSoundTypeChange: (String) -> Unit
) {
    // Observe the flows to get the current state values
    val studyDurationMin by studyDurationFlow.collectAsState()
    val minAlarmIntervalMin by minAlarmIntervalFlow.collectAsState()
    val maxAlarmIntervalMin by maxAlarmIntervalFlow.collectAsState()
    val showNextAlarmTime by showNextAlarmTimeFlow.collectAsState()
    val alarmSoundType by alarmSoundTypeFlow.collectAsState()
    val eyeRestSoundType by eyeRestSoundTypeFlow.collectAsState()
    
    // 获取系统提示音列表
    val context = LocalContext.current
    val soundOptions = remember { SoundOptions.getSoundOptions(context) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            // 使用主题的 surface 颜色，自动适应暗色模式
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.timer_configuration),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Study Duration Setting
            val studyDurationOptions = listOf(30, 45, 60, 75, 90, 105, 120)
            val formattedOptions = studyDurationOptions.associate { duration ->
                val breakDuration = calculateBreakDuration(duration)
                duration to "study $duration min + break $breakDuration min"
            }
            SettingItem(
                title = stringResource(R.string.study_duration),
                value = formattedOptions[studyDurationMin] ?: "",
                options = studyDurationOptions,
                formatOption = { duration -> formattedOptions[duration] ?: "" },
                onOptionSelected = onStudyDurationChange
            )
            
            Spacer(modifier = Modifier.height(16.dp)) // Increased spacing
            
            // Alarm Interval Settings
            Text(
                text = stringResource(R.string.alarm_configuration),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Min Alarm Interval
            val minAlarmIntervalText = "$minAlarmIntervalMin min"
            SettingItem(
                title = stringResource(R.string.min_alarm_interval),
                value = minAlarmIntervalText,
                options = listOf(1, 2, 3, 4, 5),
                formatOption = { min -> "$min min" },
                onOptionSelected = onMinAlarmIntervalChange
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Max Alarm Interval
            val maxAlarmIntervalText = "$maxAlarmIntervalMin min"
            SettingItem(
                title = stringResource(R.string.max_alarm_interval),
                value = maxAlarmIntervalText,
                options = listOf(3, 4, 5, 6, 7, 8, 9, 10),
                formatOption = { min -> "$min min" },
                onOptionSelected = onMaxAlarmIntervalChange
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            // Row for the Switch setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp), // Add some padding
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Pushes label and switch apart
            ) {
                Text(
                    text = stringResource(R.string.show_next_alarm_time),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = showNextAlarmTime,
                    onCheckedChange = onShowNextAlarmTimeChange,
                    colors = SwitchDefaults.colors( // Optional: customize colors
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            
            // 提示音设置标题
            Text(
                text = stringResource(R.string.sound_configuration),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 闹钟提示音设置
            SoundSettingItem(
                title = stringResource(R.string.alarm_sound),
                selectedSoundId = alarmSoundType,
                soundOptions = soundOptions,
                onSoundSelected = onAlarmSoundTypeChange
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 休息结束提示音设置
            SoundSettingItem(
                title = stringResource(R.string.eye_rest_sound),
                selectedSoundId = eyeRestSoundType,
                soundOptions = soundOptions,
                onSoundSelected = onEyeRestSoundTypeChange
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
                modifier = Modifier.width(200.dp) // 增加宽度以适应更长的文本
            ) {
                Text(text = value)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(200.dp) // 与按钮宽度保持一致
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

@Composable
fun SoundSettingItem(
    title: String,
    selectedSoundId: String,
    soundOptions: List<SoundOption>,
    onSoundSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // 获取当前选择的提示音名称
    val selectedSound = soundOptions.find { it.id == selectedSoundId } ?: soundOptions.firstOrNull()
    val displayName = selectedSound?.name ?: "默认提示音"
    
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
                modifier = Modifier.width(160.dp) // 稍微增加宽度以适应中文
            ) {
                Text(
                    text = displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(200.dp) // 下拉菜单稍微宽一些
            ) {
                soundOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            onSoundSelected(option.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 格式化学习时长和休息时长
 * @param studyDuration 学习时长（分钟）
 * @return 格式化后的字符串，包含学习时长和休息时长
 */
@Composable
private fun formatStudyDurationWithBreak(studyDuration: Int): String {
    // 使用与 MainActivity 相同的计算方法
    val breakDuration = calculateBreakDuration(studyDuration)
    return stringResource(R.string.study_duration_format, studyDuration, breakDuration)
}

/**
 * 计算休息时长
 * @param studyDuration 学习时长（分钟）
 * @return 休息时长（分钟）
 */
private fun calculateBreakDuration(studyDuration: Int): Int {
    val calculated = (studyDuration * (20.0 / 90.0)).roundToInt()
    return maxOf(5, calculated) // 确保最少 5 分钟休息
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    StudyTimerTheme {
        SettingsScreen(
            studyDurationFlow = MutableStateFlow(90),
            minAlarmIntervalFlow = MutableStateFlow(3),
            maxAlarmIntervalFlow = MutableStateFlow(5),
            showNextAlarmTimeFlow = MutableStateFlow(false),
            alarmSoundTypeFlow = MutableStateFlow(SoundOptions.DEFAULT_ALARM_SOUND_TYPE),
            eyeRestSoundTypeFlow = MutableStateFlow(SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE),
            onStudyDurationChange = {},
            onMinAlarmIntervalChange = {},
            onMaxAlarmIntervalChange = {},
            onShowNextAlarmTimeChange = {},
            onAlarmSoundTypeChange = {},
            onEyeRestSoundTypeChange = {},
            onNavigateBack = {},
            onNavigateToThemeSettings = {}
        )
    }
}