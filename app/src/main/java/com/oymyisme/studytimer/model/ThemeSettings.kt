package com.oymyisme.studytimer.model

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 主题模式枚举
 */
enum class ThemeMode {
    SYSTEM, // 跟随系统
    LIGHT,  // 始终亮色
    DARK    // 始终暗色
}

/**
 * 主题设置管理类
 * 
 * 负责存储和管理用户的主题偏好设置
 */
class ThemeSettings(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "theme_settings")
        
        // 数据存储键
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val AMOLED_BLACK_KEY = booleanPreferencesKey("amoled_black")
    }
    
    // 获取主题模式设置
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val themeModeString = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(themeModeString)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }
    
    // 获取动态颜色设置
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: true
    }
    
    // 获取纯黑模式设置
    val amoledBlack: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AMOLED_BLACK_KEY] ?: false
    }
    
    // 设置主题模式
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
    
    // 设置动态颜色
    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }
    
    // 设置纯黑模式
    suspend fun setAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AMOLED_BLACK_KEY] = enabled
        }
    }
}
