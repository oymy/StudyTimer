package com.oymyisme.studytimer.model

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
     * 计算休息时长
     */
    private fun calculateBreakDuration(studyDuration: Int): Int {
        val breakRatio = 2.0 / 9.0
        return (studyDuration * breakRatio).roundToInt().coerceAtLeast(5)
    }
}
