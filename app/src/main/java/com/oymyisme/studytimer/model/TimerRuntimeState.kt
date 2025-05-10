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
     * 计算当前进度（0-1）
     */
    fun calculateProgress(totalDuration: Long): Float {
        return if (totalDuration > 0 && timeLeftInSession <= totalDuration) {
            (totalDuration - timeLeftInSession).toFloat() / totalDuration
        } else {
            0f
        }
    }
    
    /**
     * 判断是否处于学习阶段
     */
    fun isStudying(): Boolean = phase == TimerPhase.STUDYING
    
    /**
     * 判断是否处于休息阶段
     */
    fun isBreak(): Boolean = phase == TimerPhase.BREAK
    
    /**
     * 判断是否处于眼睛休息阶段
     */
    fun isEyeRest(): Boolean = phase == TimerPhase.EYE_REST
    
    /**
     * 判断是否处于空闲阶段
     */
    fun isIdle(): Boolean = phase == TimerPhase.IDLE
    
    /**
     * 创建新的状态实例，更新阶段
     */
    fun copyWithPhase(newPhase: TimerPhase): TimerRuntimeState {
        return this.copy(phase = newPhase)
    }
    
    /**
     * 创建新的状态实例，更新剩余时间
     */
    fun copyWithTimeLeft(newTimeLeft: Long): TimerRuntimeState {
        return this.copy(timeLeftInSession = newTimeLeft)
    }
    
    /**
     * 创建新的状态实例，更新闹钟剩余时间
     */
    fun copyWithTimeUntilNextAlarm(newTimeUntilNextAlarm: Long): TimerRuntimeState {
        return this.copy(timeUntilNextAlarm = newTimeUntilNextAlarm)
    }
    
    /**
     * 创建新的状态实例，更新周期进度
     */
    fun copyWithElapsedTime(newElapsedTime: Long): TimerRuntimeState {
        return this.copy(elapsedTimeInFullCycle = newElapsedTime)
    }
    
    /**
     * 创建新的状态实例，更新周期完成状态
     */
    fun copyWithCycleCompleted(completed: Boolean): TimerRuntimeState {
        return this.copy(cycleCompleted = completed)
    }
}
