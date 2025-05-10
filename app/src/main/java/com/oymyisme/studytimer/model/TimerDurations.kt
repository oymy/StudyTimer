package com.oymyisme.studytimer.model

/**
 * 计时器时长数据类
 * 封装所有与计时器时长计算相关的参数
 * 
 * 提供计算方法，减少重复的时间计算逻辑
 * 遵循高内聚、低耦合的设计原则，将相关的计算逻辑集中在一个类中
 */
data class TimerDurations(
    val studyDurationMillis: Long = 0,
    val breakDurationMillis: Long = 0
) {
    /**
     * 获取总周期时长（毫秒）
     * 学习时长 + 休息时长
     */
    val totalCycleDurationMillis: Long
        get() = studyDurationMillis + breakDurationMillis
        
    /**
     * 计算周期剩余时间（毫秒）
     * 
     * @param elapsedTimeInFullCycle 已经过去的时间（毫秒）
     * @return 周期剩余时间（毫秒）
     */
    fun calculateRemainingCycleTime(elapsedTimeInFullCycle: Long): Long {
        return (totalCycleDurationMillis - elapsedTimeInFullCycle).coerceAtLeast(0)
    }
    
    /**
     * 计算周期进度（0-1）
     * 
     * @param elapsedTimeInFullCycle 已经过去的时间（毫秒）
     * @return 周期进度（0-1）
     */
    fun calculateCycleProgress(elapsedTimeInFullCycle: Long): Float {
        return if (totalCycleDurationMillis > 0) {
            (elapsedTimeInFullCycle.toFloat() / totalCycleDurationMillis).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * 计算学习阶段进度（0-1）
     * 
     * @param timeLeftInStudy 学习阶段剩余时间（毫秒）
     * @return 学习阶段进度（0-1）
     */
    fun calculateStudyProgress(timeLeftInStudy: Long): Float {
        return if (studyDurationMillis > 0) {
            (1 - timeLeftInStudy.toFloat() / studyDurationMillis).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * 计算休息阶段进度（0-1）
     * 
     * @param timeLeftInBreak 休息阶段剩余时间（毫秒）
     * @return 休息阶段进度（0-1）
     */
    fun calculateBreakProgress(timeLeftInBreak: Long): Float {
        return if (breakDurationMillis > 0) {
            (1 - timeLeftInBreak.toFloat() / breakDurationMillis).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    companion object {
        /**
         * 从 TimerSettings 创建 TimerDurations
         * 
         * @param settings 计时器设置
         * @return 计时器时长数据类实例
         */
        fun fromSettings(settings: TimerSettings): TimerDurations {
            return TimerDurations(
                studyDurationMillis = settings.studyTimeMs,
                breakDurationMillis = settings.breakTimeMs
            )
        }
    }
}
