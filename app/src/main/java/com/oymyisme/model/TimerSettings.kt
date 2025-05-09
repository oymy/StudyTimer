package com.oymyisme.model

import com.oymyisme.studytimer.SoundOptions
import com.oymyisme.studytimer.TestMode
import kotlin.math.roundToInt

/**
 * 时间单位枚举类
 * 用于指定计时器设置中的时间单位
 */
enum class TimeUnit {
    MINUTES,  // 分钟为单位，用于正常模式
    SECONDS   // 秒为单位，用于测试模式
}

/**
 * 计时器设置数据类
 * 封装所有与计时器设置相关的参数
 * 
 * 遵循高内聚、低耦合的设计原则，将相关的设置和计算逻辑集中在一个类中
 */
data class TimerSettings(
    val studyDurationMin: Int = 90,
    val minAlarmIntervalMin: Int = 3,
    val maxAlarmIntervalMin: Int = 5,
    val showNextAlarmTime: Boolean = false,
    val alarmSoundType: String = SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
    val eyeRestSoundType: String = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
    val testModeEnabled: Boolean = false,
    val timeUnit: TimeUnit = TimeUnit.MINUTES // 默认使用分钟为单位
) {
    /**
     * 计算休息时长（分钟）
     */
    val breakDurationMin: Int
        get() = calculateBreakDuration(studyDurationMin)
    
    /**
     * 获取学习时长（毫秒）
     * 根据 timeUnit 字段决定如何转换为毫秒
     */
    val studyTimeMs: Long
        get() = when {
            testModeEnabled -> TestMode.TEST_STUDY_TIME_MS
            timeUnit == TimeUnit.SECONDS -> studyDurationMin * 1000L
            else -> studyDurationMin * 60 * 1000L
        }
    
    /**
     * 获取休息时长（毫秒）
     * 根据 timeUnit 字段决定如何转换为毫秒
     */
    val breakTimeMs: Long
        get() = when {
            testModeEnabled -> TestMode.TEST_BREAK_TIME_MS
            timeUnit == TimeUnit.SECONDS -> breakDurationMin * 1000L
            else -> breakDurationMin * 60 * 1000L
        }
    
    /**
     * 获取闹钟最小间隔（毫秒）
     * 根据 timeUnit 字段决定如何转换为毫秒
     */
    val minAlarmIntervalMs: Long
        get() = when {
            testModeEnabled -> TestMode.TEST_ALARM_INTERVAL_MS
            timeUnit == TimeUnit.SECONDS -> minAlarmIntervalMin * 1000L
            else -> minAlarmIntervalMin * 60 * 1000L
        }
    
    /**
     * 获取闹钟最大间隔（毫秒）
     * 根据 timeUnit 字段决定如何转换为毫秒
     */
    val maxAlarmIntervalMs: Long
        get() = when {
            testModeEnabled -> TestMode.TEST_ALARM_INTERVAL_MS
            timeUnit == TimeUnit.SECONDS -> maxAlarmIntervalMin * 1000L
            else -> maxAlarmIntervalMin * 60 * 1000L
        }
    
    /**
     * 获取总周期时长（毫秒）
     * 包括学习时间和休息时间
     * 
     * @return 总周期时长，单位毫秒
     */
    fun getTotalCycleDurationMs(): Long {
        return studyTimeMs + breakTimeMs
    }
    
    /**
     * 获取格式化的学习时长显示字符串
     * 根据 timeUnit 字段返回不同的格式
     * 
     * @return 格式化的学习时长字符串，如“90分钟”或“30秒”
     */
    fun getFormattedStudyDuration(): String {
        return when {
            testModeEnabled -> "${TestMode.TEST_STUDY_TIME_MS / 1000}秒"
            timeUnit == TimeUnit.SECONDS -> "${studyDurationMin}秒"
            else -> "${studyDurationMin}分钟"
        }
    }
    
    /**
     * 获取格式化的学习和休息时长显示字符串
     * 根据 timeUnit 字段返回不同的格式
     * 
     * @return 格式化的学习和休息时长字符串，如“90分钟学习+20分钟休息”
     */
    fun getFormattedStudyAndBreakDuration(): String {
        return when {
            testModeEnabled -> "${TestMode.TEST_STUDY_TIME_MS / 1000}秒学习+${TestMode.TEST_BREAK_TIME_MS / 1000}秒休息"
            timeUnit == TimeUnit.SECONDS -> "${studyDurationMin}秒学习+${breakDurationMin}秒休息"
            else -> "${studyDurationMin}分钟学习+${breakDurationMin}分钟休息"
        }
    }
    
    /**
     * 验证设置是否有效
     * 
     * @return 如果设置有效返回 true，否则返回 false
     */
    fun isValidSettings(): Boolean {
        // 学习时间必须大于 0
        if (studyDurationMin <= 0) return false
        
        // 闹钟间隔必须合理
        if (minAlarmIntervalMin < 0 || maxAlarmIntervalMin < 0) return false
        if (minAlarmIntervalMin > maxAlarmIntervalMin) return false
        
        // 闹钟间隔不能超过学习时间
        if (maxAlarmIntervalMin > studyDurationMin) return false
        
        return true
    }
    
    /**
     * 创建一个新的 TimerSettings 实例，只更新学习时长
     * 
     * @param newStudyDurationMin 新的学习时长（分钟）
     * @return 新的 TimerSettings 实例
     */
    fun copyWithStudyDuration(newStudyDurationMin: Int): TimerSettings {
        return this.copy(studyDurationMin = newStudyDurationMin)
    }
    
    /**
     * 创建一个新的 TimerSettings 实例，只更新闹钟间隔
     * 
     * @param newMinAlarmIntervalMin 新的最小闹钟间隔（分钟）
     * @param newMaxAlarmIntervalMin 新的最大闹钟间隔（分钟）
     * @return 新的 TimerSettings 实例
     */
    fun copyWithAlarmInterval(newMinAlarmIntervalMin: Int, newMaxAlarmIntervalMin: Int): TimerSettings {
        return this.copy(
            minAlarmIntervalMin = newMinAlarmIntervalMin,
            maxAlarmIntervalMin = newMaxAlarmIntervalMin
        )
    }
    
    /**
     * 创建一个新的 TimerSettings 实例，只更新声音设置
     * 
     * @param newAlarmSoundType 新的闹钟声音类型
     * @param newEyeRestSoundType 新的眼睡声音类型
     * @return 新的 TimerSettings 实例
     */
    fun copyWithSoundSettings(newAlarmSoundType: String, newEyeRestSoundType: String): TimerSettings {
        return this.copy(
            alarmSoundType = newAlarmSoundType,
            eyeRestSoundType = newEyeRestSoundType
        )
    }
    
    /**
     * 创建一个新的 TimerSettings 实例，切换测试模式
     * 
     * @param enabled 是否启用测试模式
     * @return 新的 TimerSettings 实例
     */
    fun copyWithTestMode(enabled: Boolean): TimerSettings {
        return this.copy(testModeEnabled = enabled)
    }
    
    /**
     * 计算休息时长
     */
    private fun calculateBreakDuration(studyDuration: Int): Int {
        val breakRatio = 2.0 / 9.0
        return (studyDuration * breakRatio).roundToInt().coerceAtLeast(5)
    }
}
