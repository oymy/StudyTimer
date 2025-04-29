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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    studyDurationMin: Int,
    minAlarmIntervalMin: Int,
    maxAlarmIntervalMin: Int,
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
                studyDurationMin = studyDurationMin,
                minAlarmIntervalMin = minAlarmIntervalMin,
                maxAlarmIntervalMin = maxAlarmIntervalMin,
                onStudyDurationChange = onStudyDurationChange,
                onMinAlarmIntervalChange = onMinAlarmIntervalChange,
                onMaxAlarmIntervalChange = onMaxAlarmIntervalChange
            )
        }
    }
}

@Composable
fun SettingsCard(
    studyDurationMin: Int,
    minAlarmIntervalMin: Int,
    maxAlarmIntervalMin: Int,
    onStudyDurationChange: (Int) -> Unit,
    onMinAlarmIntervalChange: (Int) -> Unit,
    onMaxAlarmIntervalChange: (Int) -> Unit
) {
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
                options = listOf(30, 60, 90, 120),
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
                options = listOf(1, 2, 3, 5, 7, 10),
                formatOption = { "$it min" },
                onOptionSelected = { newMin ->
                    onMinAlarmIntervalChange(newMin)
                    // Ensure max is greater than min
                    if (newMin >= maxAlarmIntervalMin) {
                        onMaxAlarmIntervalChange(newMin + 1)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Max Alarm Interval
            SettingItem(
                title = "Maximum Interval",
                value = "$maxAlarmIntervalMin min",
                options = listOf(2, 5, 7, 10, 15, 20),
                formatOption = { "$it min" },
                onOptionSelected = { newMax ->
                    // Ensure max is greater than min
                    if (newMax > minAlarmIntervalMin) {
                        onMaxAlarmIntervalChange(newMax)
                    }
                    // Optionally, provide feedback if the selection is invalid
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
        SettingsScreen(
            studyDurationMin = 90,
            minAlarmIntervalMin = 3,
            maxAlarmIntervalMin = 5,
            onStudyDurationChange = {},
            onMinAlarmIntervalChange = {},
            onMaxAlarmIntervalChange = {},
            onNavigateBack = {}
        )
    }
}