package com.oymyisme.studytimer.model

import com.oymyisme.studytimer.timer.TimerManager

/**
 * 眼睛休息状态数据类
 * 封装所有与眼睛休息相关的状态参数
 * 
 * 遵循高内聚、低耦合的设计原则，将相关的状态集中在一个类中
 */
data class EyeRestState(
    val previousTimerPhase: TimerPhase = TimerPhase.IDLE,
    val timeLeftBeforeEyeRest: Long = 0,
    val timeUntilNextAlarmBeforeEyeRest: Long = 0
) {
    /**
     * 创建新的眼睛休息状态
     *
     * @return 新的眼睛休息状态
     */
    companion object
}
