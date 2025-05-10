package com.oymyisme.studytimer.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.oymyisme.studytimer.model.TimeUnit
import com.oymyisme.studytimer.model.TimerSettings
import com.oymyisme.studytimer.BuildConfig
import com.oymyisme.studytimer.model.SoundOptions
import com.oymyisme.studytimer.media.AudioPlayerManager
import com.oymyisme.studytimer.notification.NotificationHelper
import com.oymyisme.studytimer.timer.TimerManager
import com.oymyisme.studytimer.media.VibrationManager
import com.oymyisme.studytimer.model.TimerRuntimeState
import kotlinx.coroutines.flow.StateFlow

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
    val runtimeState: StateFlow<TimerRuntimeState>
        get() = timerManager.runtimeState

    val timeLeftInSession: Long
        get() = runtimeState.value.timeLeftInSession

    // Wake lock to keep CPU running
    private var wakeLock: PowerManager.WakeLock? = null

    // 使用TimerSettings数据类管理配置参数，提高代码内聚性
    private lateinit var timerSettings: TimerSettings

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
                // 使用 TimerSettings 数据类的属性，提高代码内聚性
                audioManager.playEyeRestSound()
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
                // 使用 TimerSettings 数据类的属性，提高代码内聚性
                audioManager.playEyeRestCompleteSound()
                vibrationManager.vibrateShort()
                updateNotification()
            }

            override fun onStudySessionFinished() {
                // 使用 TimerSettings 数据类的属性，提高代码内聚性
                audioManager.playAlarmSound()
                vibrationManager.vibrateForAlarm()
                updateNotification()
            }

            override fun onBreakFinished() {
                // 使用 TimerSettings 数据类的属性，提高代码内聚性
                audioManager.playAlarmSound()
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
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
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


                    // 创建 TimerSettings 对象
                    timerSettings = TimerSettings(
                        studyDurationMin = it.getIntExtra(
                            EXTRA_STUDY_DURATION_MIN,
                            DEFAULT_STUDY_TIME_MIN
                        ),
                        // breakDurationMin 在 TimerSettings 中是计算属性
                        minAlarmIntervalMin = it.getIntExtra(
                            EXTRA_MIN_ALARM_INTERVAL_MIN,
                            DEFAULT_MIN_ALARM_INTERVAL_MIN
                        ),
                        maxAlarmIntervalMin = it.getIntExtra(
                            EXTRA_MAX_ALARM_INTERVAL_MIN,
                            DEFAULT_MAX_ALARM_INTERVAL_MIN
                        ),
                        showNextAlarmTime = it.getBooleanExtra(EXTRA_SHOW_NEXT_ALARM_TIME, false),
                        alarmSoundType = it.getStringExtra(EXTRA_ALARM_SOUND_TYPE)
                            ?: SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
                        eyeRestSoundType = it.getStringExtra(EXTRA_EYE_REST_SOUND_TYPE)
                            ?: SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
                        testModeEnabled = if (it.hasExtra(EXTRA_TEST_MODE)) {
                            it.getBooleanExtra(EXTRA_TEST_MODE, BuildConfig.DEBUG)
                        } else {
                            BuildConfig.DEBUG
                        },
                        timeUnit = if (if (it.hasExtra(EXTRA_TEST_MODE)) {
                                it.getBooleanExtra(EXTRA_TEST_MODE, BuildConfig.DEBUG)
                            } else {
                                BuildConfig.DEBUG
                            }
                        ) TimeUnit.SECONDS else TimeUnit.MINUTES
                    )

                    if (BuildConfig.DEBUG) {
                        Log.d(
                            TAG,
                            "Starting service with study: ${timerSettings.studyDurationMin} min, break: ${timerSettings.breakDurationMin} min, testMode: ${timerSettings.testModeEnabled}"
                        )
                    }

                    // 配置 TimerManager，直接传递 TimerSettings 对象
                    timerManager.configure(timerSettings
                    )

                    // 创建初始通知，使用 TimerSettings 对象的属性
                    val initialNotification = notificationManager.createNotification(
                        TimerManager.Companion.TimerPhase.IDLE,
                        0L,
                        timerSettings.showNextAlarmTime,
                        0L
                    )

                    // 启动前台服务
                    startForeground(NOTIFICATION_ID, initialNotification)

                    if (BuildConfig.DEBUG) {
                        Log.d(
                            TAG,
                            "Started foreground service with notification ID: $NOTIFICATION_ID"
                        )
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
     * 使用单一数据源原则，提高代码的内聚性和可维护性
     */
    private fun updateNotification() {
        // 直接使用单一状态流
        val state = timerManager.runtimeState.value
        val currentState = state.phase
        val timeLeftInSession = state.timeLeftInSession
        val timeUntilNextAlarm = state.timeUntilNextAlarm

        // 使用数据类更新通知
        notificationManager.updateNotification(
            currentState,
            timeLeftInSession,
            timerSettings.showNextAlarmTime,
            timeUntilNextAlarm
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
     * 使用 TimerSettings 数据类管理设置，遵循高内聚、低耦合的设计原则
     *
     * @param enabled 是否启用测试模式
     * @param studyDurationMinutes 学习时长（分钟）
     */
    fun updateTestMode(enabled: Boolean, studyDurationMinutes: Int) {
        // 初始化默认的 TimerSettings，如果尚未初始化
        if (!::timerSettings.isInitialized) {
            timerSettings = TimerSettings(
                studyDurationMin = studyDurationMinutes,
                testModeEnabled = enabled,
                timeUnit = if (enabled) TimeUnit.SECONDS else TimeUnit.MINUTES
            )
        } else {
            // 更新现有的 TimerSettings，使用数据类的 copy 方法创建新实例
            timerSettings = timerSettings.copy(
                studyDurationMin = studyDurationMinutes,
                testModeEnabled = enabled,
                timeUnit = if (enabled) TimeUnit.SECONDS else TimeUnit.MINUTES
            )
        }

        // 更新 TimerManager 的配置，直接传递 TimerSettings 对象
        timerManager.configure(timerSettings)

        // 如果当前是空闲状态，更新显示的时间
        if (timerManager.runtimeState.value.isIdle()) {
            // 直接重新启动计时器，这将更新剩余时间
            timerManager.configure(timerSettings)
        }

        if (BuildConfig.DEBUG) {
            Log.d(
                TAG, "Test mode updated: ${timerSettings.testModeEnabled}, " +
                        "Study duration: ${timerSettings.studyDurationMin} min, " +
                        "Break duration: ${timerSettings.breakDurationMin} min"
            )
        }
    }

    // 删除了 calculateDefaultBreak 方法，因为我们已经在 TimerSettings 数据类中有了 breakDurationMin 计算属性
    // 这符合高内聚、低耦合的设计原则，将相关的计算逻辑集中在数据类中

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