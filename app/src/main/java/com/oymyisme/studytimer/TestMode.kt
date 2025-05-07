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
    
    /**
     * 测试模式的学习时长（30秒）
     */
    const val TEST_STUDY_DURATION_MIN = 0.5f
    
    /**
     * 测试模式的最小闹钟间隔（10秒，以分钟为单位表示）
     */
    const val TEST_MIN_ALARM_INTERVAL_MIN = 10.0f / 60.0f
    
    /**
     * 测试模式的最大闹钟间隔（10秒，以分钟为单位表示）
     */
    const val TEST_MAX_ALARM_INTERVAL_MIN = 10.0f / 60.0f
    
    /**
     * 测试模式的休息时长（10秒）
     */
    const val TEST_BREAK_DURATION_MIN = 10.0f / 60.0f
    
    /**
     * 获取测试模式的学习时长（分钟）
     */
    fun getStudyDurationMin(): Int {
        return 0 // 0分钟，实际使用 30 秒
    }
    
    /**
     * 获取测试模式的最小闹钟间隔（分钟）
     * 
     * 由于我们需要整数值，这里将20秒转换为0分钟
     * 实际的秒数将在 StudyTimerService 中处理
     */
    fun getMinAlarmIntervalMin(): Int {
        return 0 // 0分钟，实际使用20秒
    }
    
    /**
     * 获取测试模式的最大闹钟间隔（分钟）
     * 
     * 由于我们需要整数值，这里将20秒转换为0分钟
     * 实际的秒数将在 StudyTimerService 中处理
     */
    fun getMaxAlarmIntervalMin(): Int {
        return 0 // 0分钟，实际使用20秒
    }
    
    /**
     * 获取测试模式的休息时长（分钟）
     * 
     * 由于我们需要整数值，这里将20秒转换为0分钟
     * 实际的秒数将在 StudyTimerService 中处理
     */
    fun getBreakDurationMin(): Int {
        return 0 // 0分钟，实际使用20秒
    }
}
