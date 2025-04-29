package com.example.studytimer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studytimer.ui.theme.StudyTimerTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    studyDurationFlow: StateFlow<Int>,
    minAlarmIntervalFlow: StateFlow<Int>,
    maxAlarmIntervalFlow: StateFlow<Int>,
    onStudyDurationChange: (Int) -> Unit,
    onMinAlarmIntervalChange: (Int) -> Unit,
    onMaxAlarmIntervalChange: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                onStudyDurationChange = onStudyDurationChange,
                onMinAlarmIntervalChange = onMinAlarmIntervalChange,
                onMaxAlarmIntervalChange = onMaxAlarmIntervalChange
            )
        }
    }
}

@Composable
fun SettingsCard(
    studyDurationFlow: StateFlow<Int>,
    minAlarmIntervalFlow: StateFlow<Int>,
    maxAlarmIntervalFlow: StateFlow<Int>,
    onStudyDurationChange: (Int) -> Unit,
    onMinAlarmIntervalChange: (Int) -> Unit,
    onMaxAlarmIntervalChange: (Int) -> Unit
) {
    // Observe the flows to get the current state values
    val studyDurationMin by studyDurationFlow.collectAsState()
    val minAlarmIntervalMin by minAlarmIntervalFlow.collectAsState()
    val maxAlarmIntervalMin by maxAlarmIntervalFlow.collectAsState()

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
                onOptionSelected = { selectedDuration ->
                    // Validate before updating
                    if (selectedDuration >= minAlarmIntervalMin && selectedDuration >= maxAlarmIntervalMin) {
                        onStudyDurationChange(selectedDuration)
                    }
                }
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
                onOptionSelected = { selectedMinInterval ->
                    // Validate before updating
                    if (selectedMinInterval <= maxAlarmIntervalMin && selectedMinInterval <= studyDurationMin) {
                        onMinAlarmIntervalChange(selectedMinInterval)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Max Alarm Interval
            SettingItem(
                title = "Maximum Interval",
                value = "$maxAlarmIntervalMin min",
                options = listOf(3, 4, 5, 6, 7, 8, 9, 10),
                formatOption = { "$it min" },
                onOptionSelected = { selectedMaxInterval ->
                    // Validate before updating
                    if (selectedMaxInterval >= minAlarmIntervalMin && selectedMaxInterval <= studyDurationMin) {
                        onMaxAlarmIntervalChange(selectedMaxInterval)
                    }
                }
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    StudyTimerTheme {
        // Use MutableStateFlow for preview purposes
        SettingsScreen(
            studyDurationFlow = remember { MutableStateFlow(90) },
            minAlarmIntervalFlow = remember { MutableStateFlow(3) },
            maxAlarmIntervalFlow = remember { MutableStateFlow(5) },
            onStudyDurationChange = {},
            onMinAlarmIntervalChange = {},
            onMaxAlarmIntervalChange = {},
            onNavigateBack = {}
        )
    }
}