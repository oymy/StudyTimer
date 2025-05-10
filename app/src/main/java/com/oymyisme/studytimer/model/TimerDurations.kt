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

    companion object
}
