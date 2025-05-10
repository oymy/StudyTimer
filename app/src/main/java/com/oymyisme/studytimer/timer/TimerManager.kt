package com.oymyisme.studytimer.timer

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oymyisme.model.TimerSettings
import com.oymyisme.studytimer.BuildConfig
import com.oymyisme.studytimer.model.EyeRestState
import com.oymyisme.studytimer.model.TestMode
import com.oymyisme.studytimer.model.TimerDurations
import com.oymyisme.studytimer.model.TimerRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Random

/**
 * 计时器管理器类
 * 
 * 负责管理学习、休息和眼睛休息计时器
 * 使用状态模式和单一数据源原则重构
 */
class TimerManager : ViewModel() {
    companion object {
        const val TAG = "TimerManager"
        
        // 计时器状态
        enum class TimerPhase {
            IDLE, STUDYING, EYE_REST, BREAK
        }
    }

    // 使用单一状态流管理所有状态
    private val _runtimeState = MutableStateFlow(TimerRuntimeState())
    val runtimeState: StateFlow<TimerRuntimeState> = _runtimeState.asStateFlow()
    
    // 计时器实例管理
    private val timerInstances = TimerInstances()
    
    // 随机数生成器，用于闹钟间隔
    val random = Random()
    
    // 计时器设置
    private lateinit var timerSettings: TimerSettings
    
    // 内部计算用的时间值
    var timerDurations = TimerDurations()
        private set
    
    // 眼睛休息相关状态
    var eyeRestState = EyeRestState()
        private set
        
    // 眼睛休息时长
    val eyeRestDurationMs: Long = TestMode.TEST_EYE_REST_TIME_MS
    
    // 状态策略
    private val studyingStrategy = StudyingStateStrategy()
    private val breakStrategy = BreakStateStrategy()
    private val eyeRestStrategy = EyeRestStateStrategy()
    private val alarmStrategy = AlarmStateStrategy()
    
    // 使用 TimerSettings 数据类中的方法获取时间值
    val studyTimeMs: Long
        get() = if (::timerSettings.isInitialized) timerSettings.studyTimeMs else 0L
    
    val breakTimeMs: Long
        get() = if (::timerSettings.isInitialized) timerSettings.breakTimeMs else 0L
    
    val minAlarmIntervalMs: Long
        get() = if (::timerSettings.isInitialized) timerSettings.minAlarmIntervalMs else 0L
    
    val maxAlarmIntervalMs: Long
        get() = if (::timerSettings.isInitialized) timerSettings.maxAlarmIntervalMs else 0L
        
    val testModeEnabled: Boolean
        get() = if (::timerSettings.isInitialized) timerSettings.testModeEnabled else false

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

    var callback: TimerCallback? = null
        private set

    /**
     * 设置计时器回调
     */
    fun setCallback(callback: TimerCallback) {
        this.callback = callback
    }

    /**
     * 配置计时器
     * 使用 TimerSettings 数据类管理配置参数，提高代码内聚性
     * 
     * @param settings 计时器设置数据类实例
     */
    fun configure(settings: TimerSettings) {
        this.timerSettings = settings

        // 更新内部计算用的时间值
        updateInternalDurations()
    }
    
    /**
     * 更新运行时状态
     */
    fun updateState(update: (TimerRuntimeState) -> TimerRuntimeState) {
        _runtimeState.value = update(_runtimeState.value)
    }

    /**
     * 更新内部计算用的时间值
     * 使用 TimerDurations 数据类管理时间值，提高代码内聚性
     */
    private fun updateInternalDurations() {
        timerDurations = TimerDurations(
            studyDurationMillis = studyTimeMs,
            breakDurationMillis = breakTimeMs
        )
    }

    /**
     * 开始学习会话
     * 使用策略模式和单一数据源原则重构
     */
    fun startStudySession() {
        timerInstances.stopAll()
        
        // 使用单一方法更新状态
        updateState { state ->
            state.copy(
                phase = Companion.TimerPhase.STUDYING,
                timeLeftInSession = timerDurations.studyDurationMillis,
                elapsedTimeInFullCycle = 0L,
                cycleCompleted = false
            )
        }
        
        // 使用策略模式创建学习会话计时器
        timerInstances.sessionTimer = studyingStrategy.createTimer(this)
        
        // 安排下一次闹钟
        scheduleNextAlarm()
    }

    /**
     * 开始休息会话
     * 使用策略模式和单一数据源原则重构
     */
    fun startBreakSession() {
        timerInstances.stopAll()
        
        updateState { state ->
            state.copy(
                phase = Companion.TimerPhase.BREAK,
                timeLeftInSession = timerDurations.breakDurationMillis
            )
        }
        
        // 使用策略模式创建休息会话计时器
        timerInstances.sessionTimer = breakStrategy.createTimer(this)
    }

    /**
     * 安排下一次闹钟
     * 使用策略模式和单一数据源原则重构
     */
    fun scheduleNextAlarm() {
        timerInstances.stopAlarm()
        
        // 使用策略模式创建闹钟计时器
        timerInstances.alarmTimer = alarmStrategy.createTimer(this)
    }

    /**
     * 触发闹钟
     * 使用策略模式和单一数据源原则重构
     */
    fun triggerAlarm() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Triggering alarm for eye rest")
        }
        
        callback?.onAlarmTriggered()
        startEyeRestTimer()
        
        // 在触发闹钟后立即计划下一次闹钟，确保循环继续
        Handler(Looper.getMainLooper()).postDelayed({
            if (runtimeState.value.isStudying()) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Scheduling next alarm after eye rest")
                }
                scheduleNextAlarm()
            }
        }, eyeRestDurationMs + 1000) // 等待眼部休息结束后再计划下一次闹钟
    }

    /**
     * 开始眼睛休息计时器
     * 使用策略模式和单一数据源原则重构
     */
    fun startEyeRestTimer() {
        // 保存当前状态，以便在眼部休息结束后恢复
        eyeRestState = EyeRestState(
            previousTimerPhase = runtimeState.value.phase,
            timeLeftBeforeEyeRest = runtimeState.value.timeLeftInSession,
            timeUntilNextAlarmBeforeEyeRest = runtimeState.value.timeUntilNextAlarm
        )
        
        // 保存眼睛休息前的计时器实例
        timerInstances.saveTimersBeforeEyeRest()
        
        // 更新状态为眼睛休息
        updateState { state ->
            state.copy(
                phase = Companion.TimerPhase.EYE_REST,
                timeLeftInSession = eyeRestDurationMs
            )
        }
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Starting eye rest. Full cycle progress: ${runtimeState.value.elapsedTimeInFullCycle}")
        }
        
        callback?.onEyeRestStarted()
        
        // 使用策略模式创建眼睛休息计时器
        timerInstances.eyeRestTimer = eyeRestStrategy.createTimer(this)
    }

    /**
     * 停止所有计时器
     * 使用策略模式和单一数据源原则重构
     */
    fun stopAllTimers() {
        timerInstances.stopAll()
        
        // 重置状态
        updateState { state ->
            TimerRuntimeState() // 重置为默认状态
        }
    }

    /**
     * 重置周期完成状态
     * 使用策略模式和单一数据源原则重构
     */
    fun resetCycleCompleted() {
        updateState { state ->
            state.copy(cycleCompleted = false)
        }
    }

    /**
     * 获取当前的计时器状态
     * 使用策略模式和单一数据源原则重构
     */
    fun getCurrentState(): Companion.TimerPhase {
        return runtimeState.value.phase
    }
}
