package com.oymyisme.studytimer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


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
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
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
            containerColor = Color(0xFFF5F5F5) // Use a light grey or theme color
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Timer Configuration", // Changed title slightly
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Study Duration Setting
            SettingItem(
                title = "Study Duration",
                value = "$studyDurationMin min",
                options = listOf(30, 45, 60, 75, 90, 105, 120),
                formatOption = { "$it min" },
                onOptionSelected = onStudyDurationChange
            )
            
            Spacer(modifier = Modifier.height(16.dp)) // Increased spacing
            
            // Alarm Interval Settings
            Text(
                text = "Eye Rest Alarm Interval", // Changed title slightly
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Min Alarm Interval
            SettingItem(
                title = "Minimum Interval",
                value = "$minAlarmIntervalMin min",
                options = listOf(1, 2, 3, 4, 5),
                formatOption = { "$it min" },
                onOptionSelected = onMinAlarmIntervalChange
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Max Alarm Interval
            SettingItem(
                title = "Maximum Interval",
                value = "$maxAlarmIntervalMin min",
                options = listOf(3, 4, 5, 6, 7, 8, 9, 10),
                formatOption = { "$it min" },
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
                    text = "Show Next Alarm Time",
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
                text = "提示音设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 闹钟提示音设置
            SoundSettingItem(
                title = "闹钟提示音",
                selectedSoundId = alarmSoundType,
                soundOptions = soundOptions,
                onSoundSelected = onAlarmSoundTypeChange
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 休息结束提示音设置
            SoundSettingItem(
                title = "休息结束提示音",
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
                modifier = Modifier.width(120.dp) // Increased width slightly
            ) {
                Text(text = value)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(120.dp) // Match button width
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
            onNavigateBack = {}
        )
    }
}