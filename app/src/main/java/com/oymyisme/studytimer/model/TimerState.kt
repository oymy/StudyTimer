package com.oymyisme.model

import com.oymyisme.studytimer.timer.TimerManager

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

}
