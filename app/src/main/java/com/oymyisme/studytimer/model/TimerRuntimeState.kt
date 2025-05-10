package com.oymyisme.studytimer.model

import com.oymyisme.studytimer.timer.TimerManager.Companion.TimerPhase

/**
 * 运行时计时器状态数据类
 * 统一管理所有计时器状态
 */
data class TimerRuntimeState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val timeLeftInSession: Long = 0L,
    val timeUntilNextAlarm: Long = 0L,
    val elapsedTimeInFullCycle: Long = 0L,
    val cycleCompleted: Boolean = false
) {

    /**
     * 判断是否处于学习阶段
     */
    fun isStudying(): Boolean = phase == TimerPhase.STUDYING

    /**
     * 判断是否处于空闲阶段
     */
    fun isIdle(): Boolean = phase == TimerPhase.IDLE

}
