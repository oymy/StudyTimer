package com.oymyisme.studytimer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.oymyisme.studytimer.BuildConfig
import com.oymyisme.studytimer.audio.AudioPlayerManager
import com.oymyisme.studytimer.notification.NotificationHelper
import com.oymyisme.studytimer.timer.TimerManager
import com.oymyisme.studytimer.vibration.VibrationManager
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * 学习计时器服务
 *
 * 负责协调计时器、音频、通知和振动管理器，提供前台服务功能
 */
class StudyTimerService : Service() {
    companion object {
        private const val TAG = "StudyTimerService"
        private const val NOTIFICATION_ID = 1
        
        // Default timer constants
        private const val DEFAULT_STUDY_TIME_MIN = 90 // 90 minutes
        private const val DEFAULT_BREAK_TIME_MIN = 20 // Default break if not passed
        const val DEFAULT_MIN_ALARM_INTERVAL_MIN = 3
        const val DEFAULT_MAX_ALARM_INTERVAL_MIN = 5
        
        // Actions
        const val ACTION_START = "com.oymyisme.studytimer.action.START"
        const val ACTION_STOP = "com.oymyisme.studytimer.action.STOP"
        
        // Extra keys for intent parameters
        const val EXTRA_STUDY_DURATION_MIN = "com.oymyisme.studytimer.extra.STUDY_DURATION_MIN"
        const val EXTRA_MIN_ALARM_INTERVAL_MIN = "com.oymyisme.studytimer.extra.MIN_ALARM_INTERVAL_MIN"
        const val EXTRA_MAX_ALARM_INTERVAL_MIN = "com.oymyisme.studytimer.extra.MAX_ALARM_INTERVAL_MIN"
        const val EXTRA_SHOW_NEXT_ALARM_TIME = "com.oymyisme.studytimer.extra.SHOW_NEXT_ALARM_TIME"
        const val EXTRA_BREAK_DURATION_MIN = "com.oymyisme.studytimer.extra.BREAK_DURATION_MIN"
        const val EXTRA_ALARM_SOUND_TYPE = "com.oymyisme.studytimer.extra.ALARM_SOUND_TYPE"
        const val EXTRA_EYE_REST_SOUND_TYPE = "com.oymyisme.studytimer.extra.EYE_REST_SOUND_TYPE"
        const val EXTRA_TEST_MODE = "com.oymyisme.studytimer.extra.TEST_MODE"
    }
    
    // Binder for client communication
    private val binder = LocalBinder()
    
    // 管理器实例
    private lateinit var timerManager: TimerManager
    private lateinit var audioManager: AudioPlayerManager
    private lateinit var notificationManager: NotificationHelper
    private lateinit var vibrationManager: VibrationManager
    
    // 暴露状态流给 UI
    val timerState: StateFlow<TimerManager.TimerState>
        get() = timerManager.timerState
    
    val timeLeftInSession: StateFlow<Long>
        get() = timerManager.timeLeftInSession
    
    val timeUntilNextAlarm: StateFlow<Long>
        get() = timerManager.timeUntilNextAlarm
    
    val elapsedTimeInFullCycleMillis: StateFlow<Long>
        get() = timerManager.elapsedTimeInFullCycleMillis
    
    val cycleCompleted: StateFlow<Boolean>
        get() = timerManager.cycleCompleted
    
    // Wake lock to keep CPU running
    private var wakeLock: PowerManager.WakeLock? = null
    
    // 配置参数
    private var studyDurationMin: Int = DEFAULT_STUDY_TIME_MIN
    private var breakDurationMin: Int = DEFAULT_BREAK_TIME_MIN
    private var minAlarmIntervalMin: Int = DEFAULT_MIN_ALARM_INTERVAL_MIN
    private var maxAlarmIntervalMin: Int = DEFAULT_MAX_ALARM_INTERVAL_MIN
    private var showNextAlarmTimeInNotification: Boolean = false
    private var alarmSoundType: String = SoundOptions.DEFAULT_ALARM_SOUND_TYPE
    private var eyeRestSoundType: String = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE
    private var testMode: Boolean = BuildConfig.DEBUG
    
    /**
     * 内部类，用于客户端绑定
     */
    inner class LocalBinder : Binder() {
        fun getService(): StudyTimerService = this@StudyTimerService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Service created")
        }
        
        // 初始化管理器
        timerManager = TimerManager()
        audioManager = AudioPlayerManager.getInstance(this)
        notificationManager = NotificationHelper.getInstance(this)
        vibrationManager = VibrationManager.getInstance(this)
        
        // 设置计时器回调
        timerManager.setCallback(object : TimerManager.TimerCallback {
            override fun onTimerTick(timeLeftInSession: Long) {
                updateNotification()
            }
            
            override fun onAlarmTick(timeUntilNextAlarm: Long) {
                updateNotification()
            }
            
            override fun onAlarmTriggered() {
                audioManager.playEyeRestSound(eyeRestSoundType)
                vibrationManager.vibrateForAlarm()
                updateNotification()
            }
            
            override fun onEyeRestStarted() {
                updateNotification()
            }
            
            override fun onEyeRestTick(timeLeftInEyeRest: Long) {
                updateNotification()
            }
            
            override fun onEyeRestFinished() {
                audioManager.playEyeRestCompleteSound(eyeRestSoundType)
                vibrationManager.vibrateShort()
                updateNotification()
            }
            
            override fun onStudySessionFinished() {
                audioManager.playAlarmSound(alarmSoundType)
                vibrationManager.vibrateForAlarm()
                updateNotification()
            }
            
            override fun onBreakFinished() {
                audioManager.playAlarmSound(alarmSoundType)
                vibrationManager.vibrateForAlarm()
                updateNotification()
            }
            
            override fun onCycleCompleted() {
                updateNotification()
            }
        })
        
        // 创建通知渠道
        notificationManager.createNotificationChannel()
        
        // 获取 WakeLock 保持 CPU 运行
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StudyTimer::WakeLock"
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Service onStartCommand")
        }
        
        intent?.let {
            when (it.action) {
                ACTION_START -> {
                    // 从 Intent 中读取参数
                    studyDurationMin = it.getIntExtra(EXTRA_STUDY_DURATION_MIN, DEFAULT_STUDY_TIME_MIN)
                    breakDurationMin = it.getIntExtra(EXTRA_BREAK_DURATION_MIN, DEFAULT_BREAK_TIME_MIN)
                    minAlarmIntervalMin = it.getIntExtra(EXTRA_MIN_ALARM_INTERVAL_MIN, DEFAULT_MIN_ALARM_INTERVAL_MIN)
                    maxAlarmIntervalMin = it.getIntExtra(EXTRA_MAX_ALARM_INTERVAL_MIN, DEFAULT_MAX_ALARM_INTERVAL_MIN)
                    showNextAlarmTimeInNotification = it.getBooleanExtra(EXTRA_SHOW_NEXT_ALARM_TIME, false)
                    alarmSoundType = it.getStringExtra(EXTRA_ALARM_SOUND_TYPE) ?: SoundOptions.DEFAULT_ALARM_SOUND_TYPE
                    eyeRestSoundType = it.getStringExtra(EXTRA_EYE_REST_SOUND_TYPE) ?: SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE
                    
                    // 如果 Intent 中显式传入了 EXTRA_TEST_MODE，使用其值
                    if (it.hasExtra(EXTRA_TEST_MODE)) {
                        testMode = it.getBooleanExtra(EXTRA_TEST_MODE, BuildConfig.DEBUG)
                    }
                    
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Starting service with study: $studyDurationMin min, break: $breakDurationMin min, testMode: $testMode")
                    }
                    
                    // 配置 TimerManager
                    timerManager.configure(
                        studyDurationMin,
                        breakDurationMin,
                        minAlarmIntervalMin,
                        maxAlarmIntervalMin,
                        testMode
                    )
                    
                    // 创建初始通知
                    val initialNotification = notificationManager.createNotification(
                        TimerManager.TimerState.IDLE,
                        0L,
                        showNextAlarmTimeInNotification,
                        0L
                    )
                    
                    // 启动前台服务
                    startForeground(NOTIFICATION_ID, initialNotification)
                    
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Started foreground service with notification ID: $NOTIFICATION_ID")
                    }
                    
                    // 获取 WakeLock
                    if (wakeLock?.isHeld == false) {
                        // 计算适当的超时时间
                        val timeout = (timerManager.studyTimeMs + timerManager.breakTimeMs + 60000L).let { duration ->
                            if (duration <= 0) 3 * 60 * 60 * 1000L else duration
                        } // 如果计算结果为 0，默认使用 3 小时
                        wakeLock?.acquire(timeout)
                        
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "WakeLock acquired with timeout ${timeout}ms")
                        }
                    }
                    
                    // 开始学习会话
                    timerManager.startStudySession()
                }
                
                ACTION_STOP -> {
                    // 停止计时器
                    timerManager.stopAllTimers()
                    
                    // 停止前台服务
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Service stopped")
                    }
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification() {
        notificationManager.updateNotification(
            timerManager.timerState.value,
            timerManager.timeLeftInSession.value,
            showNextAlarmTimeInNotification,
            timerManager.timeUntilNextAlarm.value
        )
    }
    
    /**
     * 重置周期完成状态
     * 当用户选择继续下一个周期时调用
     */
    fun resetCycleCompleted() {
        timerManager.resetCycleCompleted()
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Cycle completed state reset")
        }
    }
    
    /**
     * 更新测试模式状态
     * 此方法允许在不开始新的学习周期的情况下更新测试模式状态
     * @param enabled 是否启用测试模式
     * @param studyDurationMinutes 学习时长（分钟）
     */
    fun updateTestMode(enabled: Boolean, studyDurationMinutes: Int) {
        testMode = enabled
        studyDurationMin = studyDurationMinutes
        breakDurationMin = calculateDefaultBreak(studyDurationMinutes)
        
        // 更新 TimerManager 的配置
        timerManager.configure(
            studyDurationMin,
            breakDurationMin,
            minAlarmIntervalMin,
            maxAlarmIntervalMin,
            testMode
        )
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Test mode updated: $enabled, Study duration: $studyDurationMinutes min, Break duration: $breakDurationMin min")
        }
    }
    
    /**
     * 计算默认休息时间
     * 基于学习时间计算默认的休息时间
     */
    private fun calculateDefaultBreak(studyDuration: Int): Int {
        return (studyDuration * 0.2).roundToInt().coerceAtLeast(5)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 释放 WakeLock
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        
        // 停止计时器
        timerManager.stopAllTimers()
        
        // 释放音频资源
        audioManager.releaseMediaPlayer()
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Service destroyed")
        }
    }
}
