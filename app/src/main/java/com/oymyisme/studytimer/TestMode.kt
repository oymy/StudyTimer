package com.oymyisme.studytimer

/**
 * 测试模式工具类
 * 
 * 用于在调试版本中启用测试功能，在发布版本中自动禁用
 */
object TestMode {
    /**
     * 测试模式是否启用
     */
    val isEnabled: Boolean
        get() = BuildConfig.ENABLE_TEST_MODE
    
    // 测试模式的时间常量（毫秒）
    const val TEST_STUDY_TIME_MS = 30 * 1000L       // 30秒
    const val TEST_BREAK_TIME_MS = 10 * 1000L       // 10秒
    const val TEST_ALARM_INTERVAL_MS = 10 * 1000L   // 10秒
    const val TEST_EYE_REST_TIME_MS = 10 * 1000L    // 10秒
    
    // 便捷转换方法，将毫秒转换为分钟（浮点数）
    val TEST_STUDY_DURATION_MIN: Float
        get() = TEST_STUDY_TIME_MS / (60 * 1000f)
    
    val TEST_BREAK_DURATION_MIN: Float
        get() = TEST_BREAK_TIME_MS / (60 * 1000f)
    
    val TEST_MIN_ALARM_INTERVAL_MIN: Float
        get() = TEST_ALARM_INTERVAL_MS / (60 * 1000f)
    
    val TEST_MAX_ALARM_INTERVAL_MIN: Float
        get() = TEST_ALARM_INTERVAL_MS / (60 * 1000f)
    
    /**
     * 获取测试模式的学习时长（分钟）
     * 
     * 由于我们需要整数值，这里将秒转换为分钟
     * 实际的秒数将在 StudyTimerService 中处理
     */
    fun getStudyDurationMin(): Int {
        return 0 // 0分钟，实际使用秒
    }
    
    /**
     * 获取测试模式的最小闹钟间隔（分钟）
     * 
     * 由于我们需要整数值，这里将秒转换为分钟
     * 实际的秒数将在 StudyTimerService 中处理
     */
    fun getMinAlarmIntervalMin(): Int {
        return 0 // 0分钟，实际使用秒
    }
    
    /**
     * 获取测试模式的最大闹钟间隔（分钟）
     * 
     * 由于我们需要整数值，这里将秒转换为分钟
     * 实际的秒数将在 StudyTimerService 中处理
     */
    fun getMaxAlarmIntervalMin(): Int {
        return 0 // 0分钟，实际使用秒
    }
    
    /**
     * 获取测试模式的休息时长（分钟）
     * 
     * 由于我们需要整数值，这里将秒转换为分钟
     * 实际的秒数将在 StudyTimerService 中处理
     */
    fun getBreakDurationMin(): Int {
        return 0 // 0分钟，实际使用秒
    }
    
    /**
     * 获取测试模式的学习时长（秒）
     */
    fun getStudyDurationSec(): Int {
        return (TEST_STUDY_TIME_MS / 1000).toInt() // 30秒
    }
    
    /**
     * 获取测试模式的最小闹钟间隔（秒）
     */
    fun getMinAlarmIntervalSec(): Int {
        return (TEST_ALARM_INTERVAL_MS / 1000).toInt() // 10秒
    }
    
    /**
     * 获取测试模式的最大闹钟间隔（秒）
     */
    fun getMaxAlarmIntervalSec(): Int {
        return (TEST_ALARM_INTERVAL_MS / 1000).toInt() // 10秒
    }
    
    /**
     * 获取测试模式的休息时长（秒）
     */
    fun getBreakDurationSec(): Int {
        return (TEST_BREAK_TIME_MS / 1000).toInt() // 10秒
    }
}
