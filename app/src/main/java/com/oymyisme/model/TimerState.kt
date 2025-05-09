package com.oymyisme.model

import com.oymyisme.studytimer.timer.TimerManager

/**
 * 计时器状态数据类
 * 封装所有与计时器状态相关的参数
 */
data class TimerState(
    val timerState: TimerManager.TimerState = TimerManager.TimerState.IDLE,
    val timeLeftInSession: Long = 0L,
    val timeUntilNextAlarm: Long = 0L,
    val elapsedTimeInFullCycle: Long = 0L,
    val cycleCompleted: Boolean = false
) {
    /**
     * 判断计时器是否处于空闲状态
     */
    val isIdle: Boolean
        get() = timerState == TimerManager.TimerState.IDLE
    
    /**
     * 判断计时器是否处于学习状态
     */
    val isStudying: Boolean
        get() = timerState == TimerManager.TimerState.STUDYING
    
    /**
     * 判断计时器是否处于休息状态
     */
    val isBreak: Boolean
        get() = timerState == TimerManager.TimerState.BREAK
    
    /**
     * 判断计时器是否处于眼睛休息状态
     */
    val isEyeRest: Boolean
        get() = timerState == TimerManager.TimerState.EYE_REST
}
