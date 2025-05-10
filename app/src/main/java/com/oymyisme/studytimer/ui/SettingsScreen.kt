package com.oymyisme.studytimer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import com.oymyisme.studytimer.R
import com.oymyisme.studytimer.model.SoundOption
import com.oymyisme.studytimer.model.SoundOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * 计算休息时长
 * @param studyDuration 学习时长（分钟）
 * @return 休息时长（分钟）
 */
private fun calculateBreakDuration(studyDuration: Int): Int {
    // 休息时间约为学习时间的20%到2/9
    val breakRatio = 2.0 / 9.0
    return (studyDuration * breakRatio).roundToInt()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    timerSettings: StateFlow<com.oymyisme.model.TimerSettings>,
    onSettingsChange: (com.oymyisme.model.TimerSettings) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToThemeSettings: () -> Unit
) {
    // 收集当前设置状态
    val currentSettings by timerSettings.collectAsState()
    
    // 创建本地可变状态，用于在用户交互时临时存储更改
    var localSettings by remember { mutableStateOf(currentSettings) }
    
    // 当上游设置变化时更新本地设置
    LaunchedEffect(currentSettings) {
        localSettings = currentSettings
    }
    
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings_title),
                onNavigateBack = onNavigateBack
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
            AppearanceSettingsCard(
                onNavigateToThemeSettings = onNavigateToThemeSettings
            )
            
            // 计时器设置卡片 - 使用新的数据结构
            TimerSettingsCard(
                settings = localSettings,
                onSettingsChange = { updatedSettings ->
                    localSettings = updatedSettings
                    onSettingsChange(updatedSettings)
                }
            )
        }
    }
}

/**
 * 设置页面的顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

/**
 * 外观设置卡片
 */
@Composable
private fun AppearanceSettingsCard(
    onNavigateToThemeSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
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
            ThemeSettingsOption(
                onNavigateToThemeSettings = onNavigateToThemeSettings
            )
        }
    }
}

/**
 * 主题设置选项
 */
@Composable
private fun ThemeSettingsOption(
    onNavigateToThemeSettings: () -> Unit
) {
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

@Composable
fun TimerSettingsCard(
    settings: com.oymyisme.model.TimerSettings,
    onSettingsChange: (com.oymyisme.model.TimerSettings) -> Unit
) {
    // 获取提示音选项
    val soundOptions = SoundOptions.getSoundOptions(LocalContext.current)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.timer_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 学习时间设置
            StudyDurationSection(
                studyDurationMin = settings.studyDurationMin,
                onStudyDurationChange = { newDuration ->
                    onSettingsChange(settings.copy(studyDurationMin = newDuration))
                }
            )
            
            SectionDivider()
            
            // 闹钟间隔设置
            AlarmIntervalSection(
                minAlarmIntervalMin = settings.minAlarmIntervalMin,
                maxAlarmIntervalMin = settings.maxAlarmIntervalMin,
                onMinAlarmIntervalChange = { newMinInterval ->
                    onSettingsChange(settings.copy(minAlarmIntervalMin = newMinInterval))
                },
                onMaxAlarmIntervalChange = { newMaxInterval ->
                    onSettingsChange(settings.copy(maxAlarmIntervalMin = newMaxInterval))
                }
            )
            
            SectionDivider()
            
            // 显示下一次闹钟时间开关
            ShowNextAlarmSwitch(
                showNextAlarmTime = settings.showNextAlarmTime,
                onShowNextAlarmTimeChange = { showNextAlarm ->
                    onSettingsChange(settings.copy(showNextAlarmTime = showNextAlarm))
                }
            )
            
            SectionDivider()
            
            // 提示音设置
            SoundSettingsSection(
                alarmSoundType = settings.alarmSoundType,
                eyeRestSoundType = settings.eyeRestSoundType,
                soundOptions = soundOptions,
                onAlarmSoundTypeChange = { newAlarmSoundType ->
                    onSettingsChange(settings.copy(alarmSoundType = newAlarmSoundType))
                },
                onEyeRestSoundTypeChange = { newEyeRestSoundType ->
                    onSettingsChange(settings.copy(eyeRestSoundType = newEyeRestSoundType))
                }
            )
            
            SectionDivider()
            
            // 测试模式开关
            TestModeSwitch(
                testModeEnabled = settings.testModeEnabled,
                onTestModeEnabledChange = { enabled ->
                    onSettingsChange(settings.copy(testModeEnabled = enabled))
                }
            )
        }
    }
}

/**
 * 测试模式开关
 */
@Composable
fun TestModeSwitch(
    testModeEnabled: Boolean,
    onTestModeEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.test_mode),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.test_mode_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = testModeEnabled,
            onCheckedChange = onTestModeEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/**
 * 设置部分标题
 */
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

/**
 * 分隔线
 */
@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

/**
 * 学习时间设置部分
 */
@Composable
private fun StudyDurationSection(
    studyDurationMin: Int,
    onStudyDurationChange: (Int) -> Unit
) {
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
}

/**
 * 闹钟间隔设置部分
 */
@Composable
private fun AlarmIntervalSection(
    minAlarmIntervalMin: Int,
    maxAlarmIntervalMin: Int,
    onMinAlarmIntervalChange: (Int) -> Unit,
    onMaxAlarmIntervalChange: (Int) -> Unit
) {
    // 闹钟设置标题
    Text(
        text = stringResource(R.string.alarm_configuration),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    
    // 最小闹钟间隔
    val minAlarmIntervalText = "$minAlarmIntervalMin min"
    SettingItem(
        title = stringResource(R.string.min_alarm_interval),
        value = minAlarmIntervalText,
        options = listOf(1, 2, 3, 4, 5),
        formatOption = { min -> "$min min" },
        onOptionSelected = onMinAlarmIntervalChange
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // 最大闹钟间隔
    val maxAlarmIntervalText = "$maxAlarmIntervalMin min"
    SettingItem(
        title = stringResource(R.string.max_alarm_interval),
        value = maxAlarmIntervalText,
        options = listOf(3, 4, 5, 6, 7, 8, 9, 10),
        formatOption = { min -> "$min min" },
        onOptionSelected = onMaxAlarmIntervalChange
    )
}

/**
 * 显示下一次闹钟时间开关
 */
@Composable
private fun ShowNextAlarmSwitch(
    showNextAlarmTime: Boolean,
    onShowNextAlarmTimeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.show_next_alarm_time),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = showNextAlarmTime,
            onCheckedChange = onShowNextAlarmTimeChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        )
    }
}

/**
 * 提示音设置部分
 */
@Composable
private fun SoundSettingsSection(
    alarmSoundType: String,
    eyeRestSoundType: String,
    soundOptions: List<SoundOption>,
    onAlarmSoundTypeChange: (String) -> Unit,
    onEyeRestSoundTypeChange: (String) -> Unit
) {
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

/**
 * 通用设置项组件，包含标题和下拉选项
 * @param T 选项类型
 * @param title 设置项标题
 * @param value 当前选中值的显示文本
 * @param options 可选项列表
 * @param formatOption 格式化选项的函数
 * @param onOptionSelected 选项被选中时的回调
 */
@Composable
fun <T> SettingItem(
    title: String,
    value: String,
    options: List<T>,
    formatOption: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    // 使用状态提升模式，将下拉菜单的展开状态管理在组件内部
    var expanded by remember { mutableStateOf(false) }
    
    SettingItemLayout(
        title = title,
        expanded = expanded,
        onExpandChange = { expanded = it },
        buttonContent = { Text(text = value) },
        dropdownWidth = 200.dp,
        dropdownContent = {
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
    )
}

/**
 * 提示音设置项组件
 * @param title 设置项标题
 * @param selectedSoundId 当前选中的提示音ID
 * @param soundOptions 可选提示音列表
 * @param onSoundSelected 提示音被选中时的回调
 */
@Composable
fun SoundSettingItem(
    title: String,
    selectedSoundId: String,
    soundOptions: List<SoundOption>,
    onSoundSelected: (String) -> Unit
) {
    // 使用状态提升模式，将下拉菜单的展开状态管理在组件内部
    var expanded by remember { mutableStateOf(false) }
    
    // 获取当前选择的提示音名称
    val selectedSound = soundOptions.find { it.id == selectedSoundId } ?: soundOptions.firstOrNull()
    val displayName = selectedSound?.name ?: "默认提示音"
    
    SettingItemLayout(
        title = title,
        expanded = expanded,
        onExpandChange = { expanded = it },
        buttonContent = { 
            Text(
                text = displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        dropdownWidth = 200.dp,  // 增加宽度以适应更长的文本
        dropdownContent = {
            soundOptions.forEach { soundOption ->
                DropdownMenuItem(
                    text = { Text(soundOption.name) },
                    onClick = {
                        onSoundSelected(soundOption.id)
                        expanded = false
                    }
                )
            }
        }
    )
}

/**
 * 设置项布局组件，封装共同的布局逻辑
 * @param title 设置项标题
 * @param expanded 下拉菜单是否展开
 * @param onExpandChange 下拉菜单展开状态变化回调
 * @param buttonContent 按钮内容
 * @param dropdownWidth 下拉菜单宽度
 * @param dropdownContent 下拉菜单内容
 */
@Composable
private fun SettingItemLayout(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    buttonContent: @Composable () -> Unit,
    dropdownWidth: Dp,
    dropdownContent: @Composable () -> Unit
) {
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
                onClick = { onExpandChange(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.width(dropdownWidth)
            ) {
                buttonContent()
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) },
                modifier = Modifier.width(dropdownWidth)
            ) {
                dropdownContent()
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



@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    StudyTimerTheme {
        // 创建一个默认的TimerSettings实例用于预览
        val defaultSettings = com.oymyisme.model.TimerSettings(
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            showNextAlarmTime = false,
            alarmSoundType = SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
            eyeRestSoundType = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
            testModeEnabled = false
        )
        
        SettingsScreen(
            timerSettings = MutableStateFlow(defaultSettings),
            onSettingsChange = {},
            onNavigateBack = {},
            onNavigateToThemeSettings = {}
        )
    }
}