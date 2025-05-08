package com.oymyisme.studytimer.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.oymyisme.studytimer.R
import kotlinx.coroutines.launch

/**
 * 主题设置界面
 * 
 * 允许用户选择主题模式、动态颜色和纯黑模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    themeSettings: ThemeSettings,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // 收集当前主题设置
    val currentThemeMode by themeSettings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dynamicColorEnabled by themeSettings.dynamicColor.collectAsState(initial = true)
    val amoledBlackEnabled by themeSettings.amoledBlack.collectAsState(initial = false)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
            // 主题模式选择
            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 主题模式单选按钮组
            Column(Modifier.selectableGroup()) {
                ThemeMode.values().forEach { themeMode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (themeMode == currentThemeMode),
                                onClick = {
                                    scope.launch {
                                        themeSettings.setThemeMode(themeMode)
                                    }
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween // 使内容靠两边显示
                    ) {
                        // 文本放在左侧
                        Text(
                            text = getThemeModeText(themeMode),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        // 单选按钮放在右侧
                        RadioButton(
                            selected = (themeMode == currentThemeMode),
                            onClick = null // null because we're handling the click on the row
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 动态颜色开关（仅在 Android 12+ 上显示）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.dynamic_color),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.dynamic_color_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dynamicColorEnabled,
                        onCheckedChange = {
                            scope.launch {
                                themeSettings.setDynamicColor(it)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 纯黑模式开关（仅在暗色模式下有效）
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.amoled_black),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.amoled_black_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = amoledBlackEnabled,
                    onCheckedChange = {
                        scope.launch {
                            themeSettings.setAmoledBlack(it)
                        }
                    },
                    enabled = currentThemeMode == ThemeMode.DARK || 
                             (currentThemeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
                )
            }
        }
    }
}

/**
 * 获取主题模式的显示文本
 */
@Composable
private fun getThemeModeText(themeMode: ThemeMode): String {
    return when (themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }
}

/**
 * 检查系统是否处于暗色模式
 */
@Composable
private fun isSystemInDarkTheme(): Boolean {
    return androidx.compose.foundation.isSystemInDarkTheme()
}
