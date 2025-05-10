package com.oymyisme.studytimer.timer

import android.os.CountDownTimer

/**
 * 计时器实例管理类
 * 统一管理所有计时器实例，提高内聚性
 */
class TimerInstances {
    var sessionTimer: CountDownTimer? = null
    var alarmTimer: CountDownTimer? = null
    var eyeRestTimer: CountDownTimer? = null
    
    // 眼睛休息前的计时器实例
    private var studyTimerBeforeEyeRest: CountDownTimer? = null
    private var alarmTimerBeforeEyeRest: CountDownTimer? = null
    
    /**
     * 停止所有计时器
     */
    fun stopAll() {
        stopSession()
        stopAlarm()
        stopEyeRest()
    }
    
    /**
     * 停止会话计时器
     */
    private fun stopSession() {
        sessionTimer?.cancel()
        sessionTimer = null
    }
    
    /**
     * 停止闹钟计时器
     */
    fun stopAlarm() {
        alarmTimer?.cancel()
        alarmTimer = null
    }
    
    /**
     * 停止眼睛休息计时器
     */
    private fun stopEyeRest() {
        eyeRestTimer?.cancel()
        eyeRestTimer = null
    }
    
    /**
     * 保存眼睛休息前的计时器实例
     */
    fun saveTimersBeforeEyeRest() {
        studyTimerBeforeEyeRest = sessionTimer
        alarmTimerBeforeEyeRest = alarmTimer
    }

}
