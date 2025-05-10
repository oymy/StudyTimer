package com.oymyisme.studytimer.model

import com.oymyisme.studytimer.timer.TimerManager

/**
 * 眼睛休息状态数据类
 * 封装所有与眼睛休息相关的状态参数
 * 
 * 遵循高内聚、低耦合的设计原则，将相关的状态集中在一个类中
 */
data class EyeRestState(
    val previousTimerState: TimerManager.Companion.TimerState = TimerManager.Companion.TimerState.IDLE,
    val timeLeftBeforeEyeRest: Long = 0,
    val timeUntilNextAlarmBeforeEyeRest: Long = 0
) {
    /**
     * 创建新的眼睛休息状态
     * 
     * @param previousState 进入眼睛休息前的状态
     * @param timeLeft 进入眼睛休息前的剩余时间
     * @param timeUntilNextAlarm 进入眼睛休息前的下一次闹钟时间
     * @return 新的眼睛休息状态
     */
    companion object {
        fun create(
            previousState: TimerManager.Companion.TimerState,
            timeLeft: Long,
            timeUntilNextAlarm: Long
        ): EyeRestState {
            return EyeRestState(
                previousTimerState = previousState,
                timeLeftBeforeEyeRest = timeLeft,
                timeUntilNextAlarmBeforeEyeRest = timeUntilNextAlarm
            )
        }
    }
}
