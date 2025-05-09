package com.oymyisme.studytimer

import com.oymyisme.model.TimerSettings

/**
 * 测试模式工具类
 * 
 * 用于在调试版本中启用测试功能，在发布版本中自动禁用
 * 遵循高内聚、低耦合的设计原则，将测试模式相关的常量和设置集中在一个类中
 */
object TestMode {
    /**
     * 测试模式是否启用
     * 
     * 默认值从 BuildConfig.ENABLE_TEST_MODE 获取
     * 但可以被 UI 中的开关控制
     */
    private var _isEnabled: Boolean = BuildConfig.ENABLE_TEST_MODE
    
    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            _isEnabled = value
        }
    
    // 测试模式的时间常量（毫秒）
    const val TEST_STUDY_TIME_MS = 30 * 1000L       // 30秒
    const val TEST_BREAK_TIME_MS = 10 * 1000L       // 10秒
    const val TEST_ALARM_INTERVAL_MS = 10 * 1000L   // 10秒
    const val TEST_EYE_REST_TIME_MS = 10 * 1000L    // 10秒
    
    // 便捷转换属性，将毫秒转换为分钟（浮点数）
    val TEST_STUDY_DURATION_MIN: Float
        get() = TEST_STUDY_TIME_MS / (60 * 1000f)
    
    val TEST_BREAK_DURATION_MIN: Float
        get() = TEST_BREAK_TIME_MS / (60 * 1000f)
    
    val TEST_MIN_ALARM_INTERVAL_MIN: Float
        get() = TEST_ALARM_INTERVAL_MS / (60 * 1000f)
    
    val TEST_MAX_ALARM_INTERVAL_MIN: Float
        get() = TEST_ALARM_INTERVAL_MS / (60 * 1000f)
    
    /**
     * 创建测试模式的 TimerSettings 实例
     * 封装所有测试模式相关的设置
     * 
     * @return 配置好的 TimerSettings 实例，启用测试模式
     */
    fun createTestModeSettings(): TimerSettings {
        return TimerSettings(
            studyDurationMin = (TEST_STUDY_TIME_MS / 1000).toInt(), // 测试模式下使用秒为单位
            minAlarmIntervalMin = (TEST_ALARM_INTERVAL_MS / 1000).toInt(), // 测试模式下使用秒为单位
            maxAlarmIntervalMin = (TEST_ALARM_INTERVAL_MS / 1000).toInt(), // 测试模式下使用秒为单位
            showNextAlarmTime = false, // 测试模式下默认不显示下一次闹钟时间
            alarmSoundType = SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
            eyeRestSoundType = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
            testModeEnabled = true,
            timeUnit = com.oymyisme.model.TimeUnit.SECONDS // 指定使用秒作为时间单位
        )
    }
}
