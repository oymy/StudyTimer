package com.oymyisme.model

import com.oymyisme.studytimer.SoundOptions
import com.oymyisme.studytimer.TestMode
import kotlin.math.roundToInt

/**
 * 计时器设置数据类
 * 封装所有与计时器设置相关的参数
 */
data class TimerSettings(
    val studyDurationMin: Int = 90,
    val minAlarmIntervalMin: Int = 3,
    val maxAlarmIntervalMin: Int = 5,
    val showNextAlarmTime: Boolean = false,
    val alarmSoundType: String = SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
    val eyeRestSoundType: String = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
    val testModeEnabled: Boolean = false
) {
    /**
     * 计算休息时长（分钟）
     */
    val breakDurationMin: Int
        get() = calculateBreakDuration(studyDurationMin)
    
    /**
     * 获取学习时长（毫秒）
     */
    val studyTimeMs: Long
        get() = if (testModeEnabled) TestMode.TEST_STUDY_TIME_MS
                else studyDurationMin * 60 * 1000L
    
    /**
     * 获取休息时长（毫秒）
     */
    val breakTimeMs: Long
        get() = if (testModeEnabled) TestMode.TEST_BREAK_TIME_MS
                else breakDurationMin * 60 * 1000L
    
    /**
     * 获取闹钟最小间隔（毫秒）
     */
    val minAlarmIntervalMs: Long
        get() = if (testModeEnabled) TestMode.TEST_ALARM_INTERVAL_MS
                else minAlarmIntervalMin * 60 * 1000L
    
    /**
     * 获取闹钟最大间隔（毫秒）
     */
    val maxAlarmIntervalMs: Long
        get() = if (testModeEnabled) TestMode.TEST_ALARM_INTERVAL_MS
                else maxAlarmIntervalMin * 60 * 1000L
    
    /**
     * 计算休息时长
     */
    private fun calculateBreakDuration(studyDuration: Int): Int {
        val breakRatio = 2.0 / 9.0
        return (studyDuration * breakRatio).roundToInt().coerceAtLeast(5)
    }
}
