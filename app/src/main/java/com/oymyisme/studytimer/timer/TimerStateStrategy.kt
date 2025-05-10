package com.oymyisme.studytimer.timer

import android.os.CountDownTimer
import android.util.Log
import com.oymyisme.studytimer.BuildConfig
import com.oymyisme.studytimer.model.TimerPhase

/**
 * 计时器状态策略接口
 * 使用策略模式处理不同的计时器状态
 */
interface TimerStateStrategy {
    /**
     * 创建计时器
     */
    fun createTimer(manager: TimerManager): CountDownTimer
    
    /**
     * 处理计时器tick事件
     */
    fun onTick(manager: TimerManager, millisUntilFinished: Long)
    
    /**
     * 处理计时器完成事件
     */
    fun onFinish(manager: TimerManager)
}

/**
 * 学习状态策略
 */
class StudyingStateStrategy : TimerStateStrategy {
    override fun createTimer(manager: TimerManager): CountDownTimer {
        val duration = manager.timerDurations.studyDurationMillis
        
        if (BuildConfig.DEBUG) {
            Log.d(TimerManager.TAG, "Creating study session timer. Duration: ${duration}ms")
        }
        
        return object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                onTick(manager, millisUntilFinished)
            }
            
            override fun onFinish() {
                onFinish(manager)
            }
        }.start()
    }
    
    override fun onTick(manager: TimerManager, millisUntilFinished: Long) {
        manager.updateState { state ->
            state.copy(
                timeLeftInSession = millisUntilFinished,
                elapsedTimeInFullCycle = manager.timerDurations.studyDurationMillis - millisUntilFinished
            )
        }
        manager.callback?.onTimerTick(millisUntilFinished)
    }
    
    override fun onFinish(manager: TimerManager) {
        if (BuildConfig.DEBUG) {
            Log.d(TimerManager.TAG, "Study session finished")
        }
        
        manager.updateState { state ->
            state.copy(
                timeLeftInSession = 0L,
                elapsedTimeInFullCycle = manager.timerDurations.studyDurationMillis
            )
        }
        
        manager.callback?.onStudySessionFinished()
        manager.startBreakSession()
    }
}

/**
 * 休息状态策略
 */
class BreakStateStrategy : TimerStateStrategy {
    override fun createTimer(manager: TimerManager): CountDownTimer {
        val duration = manager.timerDurations.breakDurationMillis
        
        if (BuildConfig.DEBUG) {
            Log.d(TimerManager.TAG, "Creating break session timer. Duration: ${duration}ms")
        }
        
        return object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                onTick(manager, millisUntilFinished)
            }
            
            override fun onFinish() {
                onFinish(manager)
            }
        }.start()
    }
    
    override fun onTick(manager: TimerManager, millisUntilFinished: Long) {
        manager.updateState { state ->
            state.copy(
                timeLeftInSession = millisUntilFinished,
                elapsedTimeInFullCycle = manager.timerDurations.studyDurationMillis + 
                    (manager.timerDurations.breakDurationMillis - millisUntilFinished)
            )
        }
        manager.callback?.onTimerTick(millisUntilFinished)
    }
    
    override fun onFinish(manager: TimerManager) {
        if (BuildConfig.DEBUG) {
            Log.d(TimerManager.TAG, "Break session finished")
        }
        
        manager.updateState { state ->
            state.copy(
                timeLeftInSession = 0L,
                elapsedTimeInFullCycle = manager.timerDurations.totalCycleDurationMillis,
                timerPhase = TimerPhase.IDLE,
                cycleCompleted = true
            )
        }
        
        manager.callback?.onBreakFinished()
        manager.callback?.onCycleCompleted()
    }
}

/**
 * 眼睛休息状态策略
 */
class EyeRestStateStrategy : TimerStateStrategy {
    override fun createTimer(manager: TimerManager): CountDownTimer {
        val duration = manager.eyeRestDurationMs
        
        if (BuildConfig.DEBUG) {
            Log.d(TimerManager.TAG, "Creating eye rest timer. Duration: ${duration}ms")
        }
        
        return object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                onTick(manager, millisUntilFinished)
            }
            
            override fun onFinish() {
                onFinish(manager)
            }
        }.start()
    }
    
    override fun onTick(manager: TimerManager, millisUntilFinished: Long) {
        manager.updateState { state ->
            state.copy(timeLeftInSession = millisUntilFinished)
        }
        manager.callback?.onEyeRestTick(millisUntilFinished)
        
        // 只在开发阶段每3秒记录一次眼部休息状态
        if (BuildConfig.DEBUG && millisUntilFinished % 3000 < 1000) {
            Log.d(TimerManager.TAG, "Eye rest ongoing: ${millisUntilFinished / 1000} seconds remaining")
        }
    }
    
    override fun onFinish(manager: TimerManager) {
        if (BuildConfig.DEBUG) {
            Log.d(TimerManager.TAG, "Eye rest finished")
        }
        
        manager.updateState { state ->
            state.copy(
                timeLeftInSession = 0L,
                timerPhase = manager.eyeRestState.previousTimerPhase
            )
        }
        
        manager.callback?.onEyeRestFinished()
        
        if (manager.eyeRestState.previousTimerPhase == TimerPhase.STUDYING) {
            // 恢复显示的剩余学习时间
            manager.updateState { state ->
                state.copy(
                    timeLeftInSession = manager.eyeRestState.timeLeftBeforeEyeRest,
                    timeUntilNextAlarm = manager.eyeRestState.timeUntilNextAlarmBeforeEyeRest
                )
            }
            
            if (BuildConfig.DEBUG) {
                Log.d(TimerManager.TAG, "Resumed study display after eye rest")
            }
        } else {
            // 如果之前的状态不是 STUDYING，进入 IDLE 状态
            if (BuildConfig.DEBUG) {
                Log.w(TimerManager.TAG, "Eye rest finished, but previous state was not STUDYING")
            }
            manager.stopAllTimers()
        }
    }
}

/**
 * 闹钟状态策略
 */
class AlarmStateStrategy : TimerStateStrategy {
    override fun createTimer(manager: TimerManager): CountDownTimer {
        // 生成随机间隔
        val minMs = manager.minAlarmIntervalMs
        val maxMs = manager.maxAlarmIntervalMs
        val range = (maxMs - minMs).toInt()
        
        // 确保范围为正数
        val interval = if (range > 0) {
            minMs + manager.random.nextInt(range)
        } else {
            minMs
        }
        
        manager.updateState { state ->
            state.copy(timeUntilNextAlarm = interval)
        }
        
        // 只在开发阶段记录闹钟计划信息
        if (BuildConfig.DEBUG) {
            val minutes = interval / (60 * 1000)
            val seconds = (interval % (60 * 1000)) / 1000
            Log.d(TimerManager.TAG, "Scheduling next alarm in $minutes min $seconds sec (${interval}ms)")
        }
        
        return object : CountDownTimer(interval, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                onTick(manager, millisUntilFinished)
            }
            
            override fun onFinish() {
                onFinish(manager)
            }
        }.start()
    }
    
    override fun onTick(manager: TimerManager, millisUntilFinished: Long) {
        manager.updateState { state ->
            state.copy(timeUntilNextAlarm = millisUntilFinished)
        }
        manager.callback?.onAlarmTick(millisUntilFinished)
        
        // 只在开发阶段每30秒记录一次闹钟剩余时间
        if (BuildConfig.DEBUG && millisUntilFinished % 30000 < 1000) {
            val remainingMin = millisUntilFinished / (60 * 1000)
            val remainingSec = (millisUntilFinished % (60 * 1000)) / 1000
            Log.d(TimerManager.TAG, "Alarm countdown: $remainingMin min $remainingSec sec remaining")
        }
    }
    
    override fun onFinish(manager: TimerManager) {
        if (BuildConfig.DEBUG) {
            Log.d(TimerManager.TAG, "Alarm timer finished")
        }
        
        if (manager.runtimeState.value.isStudying) {
            if (BuildConfig.DEBUG) {
                Log.d(TimerManager.TAG, "Triggering eye rest alarm")
            }
            manager.triggerAlarm()
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TimerManager.TAG, "Not triggering alarm because state is not STUDYING")
            }
        }
    }
}
