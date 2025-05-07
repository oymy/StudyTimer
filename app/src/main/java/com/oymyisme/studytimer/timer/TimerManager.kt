package com.oymyisme.studytimer.timer

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.oymyisme.studytimer.BuildConfig
import com.oymyisme.studytimer.TestMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

/**
 * 计时器管理器类
 * 
 * 负责管理学习、休息和眼睛休息计时器
 */
class TimerManager {
    companion object {
        private const val TAG = "TimerManager"
    }

    // 计时器状态
    enum class TimerState {
        IDLE, STUDYING, EYE_REST, BREAK
    }

    // 计时器
    private var sessionTimer: CountDownTimer? = null
    private var alarmTimer: CountDownTimer? = null
    private var eyeRestTimer: CountDownTimer? = null

    // 状态流
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    private val _timeLeftInSession = MutableStateFlow(0L)
    val timeLeftInSession: StateFlow<Long> = _timeLeftInSession.asStateFlow()
    
    private val _timeUntilNextAlarm = MutableStateFlow(0L)
    val timeUntilNextAlarm: StateFlow<Long> = _timeUntilNextAlarm.asStateFlow()
    
    private val _elapsedTimeInFullCycleMillis = MutableStateFlow(0L)
    val elapsedTimeInFullCycleMillis: StateFlow<Long> = _elapsedTimeInFullCycleMillis.asStateFlow()
    
    private val _cycleCompleted = MutableStateFlow(false)
    val cycleCompleted: StateFlow<Boolean> = _cycleCompleted.asStateFlow()

    // 随机数生成器，用于闹钟间隔
    private val random = Random()

    // 配置
    private var testMode: Boolean = false
    private var studyDurationMin: Int = 0
    private var breakDurationMin: Int = 0
    private var minAlarmIntervalMin: Int = 0
    private var maxAlarmIntervalMin: Int = 0

    // 眼睛休息前的状态保存
    private var _previousTimerStateBeforeEyeRest: TimerState = TimerState.IDLE
    private var _timeLeftBeforeEyeRest: Long = 0
    private var _timeUntilNextAlarmBeforeEyeRest: Long = 0
    private var _studyTimerBeforeEyeRest: CountDownTimer? = null
    private var _alarmTimerBeforeEyeRest: CountDownTimer? = null

    // 计算的时间值（毫秒）
    val studyTimeMs: Long
        get() = if (testMode) {
            TestMode.TEST_STUDY_TIME_MS
        } else {
            studyDurationMin * 60 * 1000L
        }
    
    val breakTimeMs: Long
        get() = if (testMode) {
            TestMode.TEST_BREAK_TIME_MS
        } else {
            breakDurationMin * 60 * 1000L
        }
    
    val minAlarmIntervalMs: Long
        get() = if (testMode) {
            TestMode.TEST_ALARM_INTERVAL_MS
        } else {
            minAlarmIntervalMin * 60 * 1000L
        }
    
    val maxAlarmIntervalMs: Long
        get() = if (testMode) {
            TestMode.TEST_ALARM_INTERVAL_MS
        } else {
            maxAlarmIntervalMin * 60 * 1000L
        }

    // 内部计算用的时间值
    private var mStudyDurationMillis: Long = 0
    private var mBreakDurationMillis: Long = 0
    private var mTotalCycleDurationMillis: Long = 0

    // 回调接口
    interface TimerCallback {
        fun onTimerTick(timeLeftInSession: Long)
        fun onAlarmTick(timeUntilNextAlarm: Long)
        fun onAlarmTriggered()
        fun onEyeRestStarted()
        fun onEyeRestTick(timeLeftInEyeRest: Long)
        fun onEyeRestFinished()
        fun onStudySessionFinished()
        fun onBreakFinished()
        fun onCycleCompleted()
    }

    private var callback: TimerCallback? = null

    /**
     * 设置计时器回调
     */
    fun setCallback(callback: TimerCallback) {
        this.callback = callback
    }

    /**
     * 配置计时器
     */
    fun configure(
        studyDurationMin: Int,
        breakDurationMin: Int,
        minAlarmIntervalMin: Int,
        maxAlarmIntervalMin: Int,
        testMode: Boolean
    ) {
        this.studyDurationMin = studyDurationMin
        this.breakDurationMin = breakDurationMin
        this.minAlarmIntervalMin = minAlarmIntervalMin
        this.maxAlarmIntervalMin = maxAlarmIntervalMin
        this.testMode = testMode

        // 更新内部计算用的时间值
        updateInternalDurations()
    }

    /**
     * 更新内部计算用的时间值
     */
    private fun updateInternalDurations() {
        mStudyDurationMillis = studyTimeMs
        mBreakDurationMillis = breakTimeMs
        mTotalCycleDurationMillis = mStudyDurationMillis + mBreakDurationMillis
    }

    /**
     * 开始学习会话
     */
    fun startStudySession() {
        stopAllTimers()
        _timerState.value = TimerState.STUDYING
        _timeLeftInSession.value = studyTimeMs
        _elapsedTimeInFullCycleMillis.value = 0L // 重置周期进度
        _cycleCompleted.value = false

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Starting study session. Duration: ${studyTimeMs}ms")
        }

        sessionTimer = object : CountDownTimer(studyTimeMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInSession.value = millisUntilFinished
                _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis - millisUntilFinished
                callback?.onTimerTick(millisUntilFinished)
            }
            
            override fun onFinish() {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Study session finished")
                }
                _timeLeftInSession.value = 0L
                _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis
                callback?.onStudySessionFinished()
                startBreakSession()
            }
        }.start()

        scheduleNextAlarm()
    }

    /**
     * 开始休息会话
     */
    private fun startBreakSession() {
        _timerState.value = TimerState.BREAK
        _timeLeftInSession.value = breakTimeMs

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Starting break session. Duration: ${breakTimeMs}ms")
        }

        sessionTimer = object : CountDownTimer(breakTimeMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInSession.value = millisUntilFinished
                _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis + (mBreakDurationMillis - millisUntilFinished)
                callback?.onTimerTick(millisUntilFinished)
            }
            
            override fun onFinish() {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Break session finished")
                }
                _timeLeftInSession.value = 0L
                _elapsedTimeInFullCycleMillis.value = mTotalCycleDurationMillis
                callback?.onBreakFinished()
                
                // 通知UI周期已完成
                _cycleCompleted.value = true
                callback?.onCycleCompleted()
                
                // 重置为IDLE状态
                _timerState.value = TimerState.IDLE
            }
        }.start()
    }

    /**
     * 安排下一次闹钟
     */
    fun scheduleNextAlarm() {
        alarmTimer?.cancel()
        
        // 生成随机间隔
        val minMs = minAlarmIntervalMs
        val maxMs = maxAlarmIntervalMs
        val range = (maxMs - minMs).toInt()
        
        // 确保范围为正数
        val interval = if (range > 0) {
            minMs + random.nextInt(range)
        } else {
            minMs
        }
        
        _timeUntilNextAlarm.value = interval
        
        // 只在开发阶段记录闹钟计划信息
        if (BuildConfig.DEBUG) {
            val minutes = interval / (60 * 1000)
            val seconds = (interval % (60 * 1000)) / 1000
            Log.d(TAG, "Scheduling next alarm in $minutes min $seconds sec (${interval}ms)")
        }
        
        alarmTimer = object : CountDownTimer(interval, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeUntilNextAlarm.value = millisUntilFinished
                callback?.onAlarmTick(millisUntilFinished)
                
                // 只在开发阶段每30秒记录一次闹钟剩余时间
                if (BuildConfig.DEBUG && millisUntilFinished % 30000 < 1000) {
                    val remainingMin = millisUntilFinished / (60 * 1000)
                    val remainingSec = (millisUntilFinished % (60 * 1000)) / 1000
                    Log.d(TAG, "Alarm countdown: $remainingMin min $remainingSec sec remaining")
                }
            }
            
            override fun onFinish() {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Alarm timer finished")
                }
                if (_timerState.value == TimerState.STUDYING) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Triggering eye rest alarm")
                    }
                    triggerAlarm()
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Not triggering alarm because state is not STUDYING")
                    }
                }
            }
        }.start()
    }

    /**
     * 触发闹钟
     */
    private fun triggerAlarm() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Triggering alarm for eye rest")
        }
        
        callback?.onAlarmTriggered()
        startEyeRestTimer()
        
        // 在触发闹钟后立即计划下一次闹钟，确保循环继续
        Handler(Looper.getMainLooper()).postDelayed({
            if (_timerState.value == TimerState.STUDYING) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Scheduling next alarm after eye rest")
                }
                scheduleNextAlarm()
            }
        }, TestMode.TEST_EYE_REST_TIME_MS + 1000) // 等待眼部休息结束后再计划下一次闹钟
    }

    /**
     * 开始眼睛休息计时器
     */
    private fun startEyeRestTimer() {
        // 保存当前状态，以便在眼部休息结束后恢复
        _previousTimerStateBeforeEyeRest = _timerState.value
        _timeLeftBeforeEyeRest = _timeLeftInSession.value
        _timeUntilNextAlarmBeforeEyeRest = _timeUntilNextAlarm.value
        _studyTimerBeforeEyeRest = sessionTimer
        _alarmTimerBeforeEyeRest = alarmTimer
        
        // 切换到眼部休息状态
        _timerState.value = TimerState.EYE_REST
        _timeLeftInSession.value = TestMode.TEST_EYE_REST_TIME_MS
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Starting eye rest. Full cycle progress: ${_elapsedTimeInFullCycleMillis.value}")
        }
        
        callback?.onEyeRestStarted()
        
        eyeRestTimer = object : CountDownTimer(TestMode.TEST_EYE_REST_TIME_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInSession.value = millisUntilFinished
                callback?.onEyeRestTick(millisUntilFinished)
                
                // 只在开发阶段每3秒记录一次眼部休息状态
                if (BuildConfig.DEBUG && millisUntilFinished % 3000 < 1000) {
                    Log.d(TAG, "Eye rest ongoing: ${millisUntilFinished / 1000} seconds remaining")
                }
            }
            
            override fun onFinish() {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Eye rest finished")
                }
                
                _timeLeftInSession.value = 0L
                callback?.onEyeRestFinished()
                
                // 恢复到之前的状态（应该是 STUDYING）
                _timerState.value = _previousTimerStateBeforeEyeRest
                
                if (_previousTimerStateBeforeEyeRest == TimerState.STUDYING) {
                    // 恢复显示的剩余学习时间
                    _timeLeftInSession.value = _timeLeftBeforeEyeRest
                    _timeUntilNextAlarm.value = _timeUntilNextAlarmBeforeEyeRest
                    
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Resumed study display after eye rest")
                    }
                } else {
                    // 如果之前的状态不是 STUDYING，进入 IDLE 状态
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Eye rest finished, but previous state was not STUDYING")
                    }
                    stopAllTimers()
                }
            }
        }.start()
    }

    /**
     * 停止所有计时器
     */
    fun stopAllTimers() {
        sessionTimer?.cancel()
        sessionTimer = null
        
        alarmTimer?.cancel()
        alarmTimer = null
        
        eyeRestTimer?.cancel()
        eyeRestTimer = null
        
        _timerState.value = TimerState.IDLE
        _timeLeftInSession.value = 0L
        _timeUntilNextAlarm.value = 0L
        _elapsedTimeInFullCycleMillis.value = 0L
    }

    /**
     * 重置周期完成状态
     */
    fun resetCycleCompleted() {
        _cycleCompleted.value = false
    }

    /**
     * 获取当前的计时器状态
     */
    fun getCurrentState(): TimerState {
        return _timerState.value
    }
}
