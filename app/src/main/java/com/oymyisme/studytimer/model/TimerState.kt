package com.oymyisme.model

import com.oymyisme.studytimer.timer.TimerManager
import java.util.concurrent.TimeUnit

/**
 * 计时器状态数据类
 * 封装所有与计时器状态相关的参数
 * 
 * 遵循高内聚、低耦合的设计原则，将相关的状态和计算逻辑集中在一个类中
 */
data class TimerState(
    val timerPhase: TimerManager.Companion.TimerPhase = TimerManager.Companion.TimerPhase.IDLE,
    val timeLeftInSession: Long = 0L,
    val timeUntilNextAlarm: Long = 0L,
    val elapsedTimeInFullCycle: Long = 0L,
    val cycleCompleted: Boolean = false
) {
    
    companion object {
        /**
         * 创建空闲状态的TimerState对象
         * 
         * @param timeLeftInSession 当前会话剩余时间（毫秒）
         * @param elapsedTimeInFullCycle 完整周期中已经过去的时间（毫秒）
         * @param cycleCompleted 周期是否完成
         * @return 空闲状态的TimerState对象
         */
        fun idle(timeLeftInSession: Long = 0L, elapsedTimeInFullCycle: Long = 0L, cycleCompleted: Boolean = false): TimerState {
            return TimerState(
                timerPhase = TimerManager.Companion.TimerPhase.IDLE,
                timeLeftInSession = timeLeftInSession,
                timeUntilNextAlarm = 0L,
                elapsedTimeInFullCycle = elapsedTimeInFullCycle,
                cycleCompleted = cycleCompleted
            )
        }
        
        /**
         * 创建学习状态的TimerState对象
         * 
         * @param timeLeftInSession 当前会话剩余时间（毫秒）
         * @param timeUntilNextAlarm 到下一次闹钟的时间（毫秒）
         * @param elapsedTimeInFullCycle 完整周期中已经过去的时间（毫秒）
         * @return 学习状态的TimerState对象
         */
        fun studying(timeLeftInSession: Long, timeUntilNextAlarm: Long, elapsedTimeInFullCycle: Long): TimerState {
            return TimerState(
                timerPhase = TimerManager.Companion.TimerPhase.STUDYING,
                timeLeftInSession = timeLeftInSession,
                timeUntilNextAlarm = timeUntilNextAlarm,
                elapsedTimeInFullCycle = elapsedTimeInFullCycle,
                cycleCompleted = false
            )
        }
        
        /**
         * 创建休息状态的TimerState对象
         * 
         * @param timeLeftInSession 当前会话剩余时间（毫秒）
         * @param elapsedTimeInFullCycle 完整周期中已经过去的时间（毫秒）
         * @return 休息状态的TimerState对象
         */
        fun breakState(timeLeftInSession: Long, elapsedTimeInFullCycle: Long): TimerState {
            return TimerState(
                timerPhase = TimerManager.Companion.TimerPhase.BREAK,
                timeLeftInSession = timeLeftInSession,
                timeUntilNextAlarm = 0L,
                elapsedTimeInFullCycle = elapsedTimeInFullCycle,
                cycleCompleted = false
            )
        }
        
        /**
         * 创建眼部休息状态的TimerState对象
         * 
         * @param timeLeftInSession 当前会话剩余时间（毫秒）
         * @param elapsedTimeInFullCycle 完整周期中已经过去的时间（毫秒）
         * @return 眼部休息状态的TimerState对象
         */
        fun eyeRest(timeLeftInSession: Long, elapsedTimeInFullCycle: Long): TimerState {
            return TimerState(
                timerPhase = TimerManager.Companion.TimerPhase.EYE_REST,
                timeLeftInSession = timeLeftInSession,
                timeUntilNextAlarm = 0L,
                elapsedTimeInFullCycle = elapsedTimeInFullCycle,
                cycleCompleted = false
            )
        }
    }
    /**
     * 判断计时器是否处于空闲状态
     */
    val isIdle: Boolean
        get() = timerPhase == TimerManager.Companion.TimerPhase.IDLE
    
    /**
     * 判断计时器是否处于学习状态
     */
    val isStudying: Boolean
        get() = timerPhase == TimerManager.Companion.TimerPhase.STUDYING
    
    /**
     * 判断计时器是否处于休息状态
     */
    val isBreak: Boolean
        get() = timerPhase == TimerManager.Companion.TimerPhase.BREAK
    
    /**
     * 判断计时器是否处于眼睛休息状态
     */
    val isEyeRest: Boolean
        get() = timerPhase == TimerManager.Companion.TimerPhase.EYE_REST
    
    /**
     * 判断计时器是否处于学习阶段（包括学习和眼睛休息）
     */
    fun isInStudyPhase(): Boolean {
        return isStudying || isEyeRest
    }
    
    /**
     * 判断计时器是否处于休息阶段
     */
    fun isInBreakPhase(): Boolean {
        return isBreak
    }
    
    /**
     * 判断计时器是否处于活动状态（非空闲）
     */
    fun isActive(): Boolean {
        return !isIdle
    }
    
    /**
     * 获取格式化的剩余时间字符串
     * 
     * @return 格式化的剩余时间，格式为 "mm:ss"
     */
    fun getFormattedTimeLeft(): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInSession)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInSession) - 
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * 获取格式化的到下次闹钟的剩余时间字符串
     * 
     * @return 格式化的到下次闹钟的剩余时间，格式为 "mm:ss"
     */
    fun getFormattedTimeUntilNextAlarm(): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeUntilNextAlarm)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeUntilNextAlarm) - 
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * 计算当前进度百分比
     * 
     * @param totalCycleDurationMs 总周期时长（毫秒）
     * @return 当前进度百分比（0-100）
     */
    fun getProgress(totalCycleDurationMs: Long): Float {
        if (totalCycleDurationMs <= 0 || isIdle) return 0f
        
        val progress = elapsedTimeInFullCycle.toFloat() / totalCycleDurationMs.toFloat() * 100f
        return progress.coerceIn(0f, 100f)
    }
    
    /**
     * 计算当前阶段的进度百分比
     * 
     * @param phaseDurationMs 当前阶段的总时长（毫秒）
     * @return 当前阶段的进度百分比（0-100）
     */
    fun getPhaseProgress(phaseDurationMs: Long): Float {
        if (phaseDurationMs <= 0 || isIdle) return 0f
        
        val elapsedInPhase = phaseDurationMs - timeLeftInSession
        val progress = elapsedInPhase.toFloat() / phaseDurationMs.toFloat() * 100f
        return progress.coerceIn(0f, 100f)
    }
    
    /**
     * 创建一个新的 TimerState 实例，更新剩余时间
     * 
     * @param newTimeLeftInSession 新的剩余时间（毫秒）
     * @return 新的 TimerState 实例
     */
    fun copyWithTimeLeft(newTimeLeftInSession: Long): TimerState {
        return this.copy(timeLeftInSession = newTimeLeftInSession)
    }
    
    /**
     * 创建一个新的 TimerState 实例，更新计时器状态
     * 
     * @param newTimerPhase 新的计时器状态
     * @return 新的 TimerState 实例
     */
    fun copyWithTimerState(newTimerPhase: TimerManager.Companion.TimerPhase): TimerState {
        return this.copy(timerPhase = newTimerPhase)
    }
    
    /**
     * 创建一个新的 TimerState 实例，更新周期完成状态
     * 
     * @param completed 周期是否完成
     * @return 新的 TimerState 实例
     */
    fun copyWithCycleCompleted(completed: Boolean): TimerState {
        return this.copy(cycleCompleted = completed)
    }
}
