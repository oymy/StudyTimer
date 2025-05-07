package com.oymyisme.studytimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.oymyisme.studytimer.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class StudyTimerService : Service() {
    companion object {
        private const val TAG = "StudyTimerService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "study_timer_channel"
        
        // Default timer constants
        private const val DEFAULT_STUDY_TIME_MIN = 90 // 90 minutes
        private const val DEFAULT_BREAK_TIME_MIN = 20 // Default break if not passed
        const val EYE_REST_TIME_MS = 10 * 1000L // 10 seconds - fixed
        const val DEFAULT_MIN_ALARM_INTERVAL_MIN = 3
        const val DEFAULT_MAX_ALARM_INTERVAL_MIN = 5
        
        // Fixed constants
        
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
        
        // 测试模式的常量 - 缩短一半时长
        private const val TEST_ALARM_INTERVAL_MS = 10 * 1000L // 10秒
        private const val TEST_BREAK_TIME_MS = 10 * 1000L // 10秒

        // Vibration Patterns
        val VIBRATE_PATTERN_ALARM = longArrayOf(0, 1000, 500, 1000) // Wait 0ms, Vibrate 1s, Pause 0.5s, Vibrate 1s
        val VIBRATE_PATTERN_SHORT = longArrayOf(0, 300)            // Wait 0ms, Vibrate 0.3s
    }
    
    // Timer state
    enum class TimerState {
        IDLE, STUDYING, EYE_REST, BREAK
    }
    
    // Binder for client communication
    private val binder = LocalBinder()
    
    // StateFlow for UI updates
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    private val _timeLeftInSession = MutableStateFlow(0L)
    val timeLeftInSession: StateFlow<Long> = _timeLeftInSession.asStateFlow()
    
    private val _timeUntilNextAlarm = MutableStateFlow(0L)
    val timeUntilNextAlarm: StateFlow<Long> = _timeUntilNextAlarm.asStateFlow()

    // New StateFlow for full cycle progress
    private val _elapsedTimeInFullCycleMillis = MutableStateFlow(0L)
    val elapsedTimeInFullCycleMillis: StateFlow<Long> = _elapsedTimeInFullCycleMillis.asStateFlow()
    
    // Timers
    private var sessionTimer: CountDownTimer? = null
    private var alarmTimer: CountDownTimer? = null
    private var eyeRestTimer: CountDownTimer? = null
    
    // Wake lock to keep CPU running
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Random for alarm intervals
    private val random = Random()
    
    // Configurable durations (in minutes)
    private var studyDurationMin: Int = DEFAULT_STUDY_TIME_MIN
    private var breakDurationMin: Int = DEFAULT_BREAK_TIME_MIN
    private var minAlarmIntervalMin: Int = DEFAULT_MIN_ALARM_INTERVAL_MIN
    private var maxAlarmIntervalMin: Int = DEFAULT_MAX_ALARM_INTERVAL_MIN
    
    // Calculated time values in milliseconds
    private val studyTimeMs: Long
        get() = if (testMode) {
            // 测试模式下使用 30 秒
            30 * 1000L
        } else {
            studyDurationMin * 60 * 1000L
        }
    
    private val breakTimeMs: Long
        get() = if (testMode) {
            // 测试模式下使用 20 秒
            TEST_BREAK_TIME_MS
        } else {
            breakDurationMin * 60 * 1000L
        }
    
    private val minAlarmIntervalMs: Long
        get() = if (testMode) {
            // 测试模式下使用 20 秒
            TEST_ALARM_INTERVAL_MS
        } else {
            minAlarmIntervalMin * 60 * 1000L
        }
    
    private val maxAlarmIntervalMs: Long
        get() = if (testMode) {
            // 测试模式下使用 20 秒
            TEST_ALARM_INTERVAL_MS
        } else {
            maxAlarmIntervalMin * 60 * 1000L
        }
    
    private var showNextAlarmTimeInNotification: Boolean = false // Default value
    
    // 提示音类型
    private var alarmSoundType: String = SoundOptions.DEFAULT_ALARM_SOUND_TYPE
    private var eyeRestSoundType: String = SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE
    
    // 测试模式 - 默认开启
    private var testMode: Boolean = true
    
    private lateinit var audioManager: AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var eyeRestMediaPlayer: MediaPlayer? = null
    private var breakMediaPlayer: MediaPlayer? = null
    
    // Durations for the current full cycle
    private var mStudyDurationMillis: Long = 0L
    private var mBreakDurationMillis: Long = 0L
    private var mTotalCycleDurationMillis: Long = 0L
    
    inner class LocalBinder : Binder() {
        fun getService(): StudyTimerService = this@StudyTimerService
    }
    
    @Suppress("DEPRECATION") // For isWiredHeadsetOn
    private fun isHeadsetConnected(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.isWiredHeadsetOn) {
            Log.d(TAG, "isHeadsetConnected: Wired headset ON")
            return true
        }
        // For Bluetooth, checking isBluetoothA2dpOn or isBluetoothScoOn can be indicative,
        // but for more robust checks, especially on newer APIs, iterating through devices is better.
        if (audioManager.isBluetoothA2dpOn) {
            Log.d(TAG, "isHeadsetConnected: Bluetooth A2DP potentially active")
            // return true; // Could return true here if A2DP on is sufficient
        }
        if (audioManager.isBluetoothScoOn) {
            Log.d(TAG, "isHeadsetConnected: Bluetooth SCO active")
            return true;
        }
    
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        Log.d(TAG, "isHeadsetConnected: Headset found via device list (Type: ${device.type})")
                        return true
                    }
                }
            }
        }
        Log.d(TAG, "isHeadsetConnected: No headset detected")
        return false
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Create notification channel
        createNotificationChannel()

        // Initialize durations immediately (they depend on testMode which might be set later,
        // but default values are fine for initial _elapsedTimeInFullCycleMillis setup for IDLE)
        updateCurrentDurationsInternal() 

        // Initialize wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StudyTimer:StudyTimerWakeLock"
        )

        // If starting in IDLE state, set progress appropriately
        if (_timerState.value == TimerState.IDLE) {
            _timeLeftInSession.value = mStudyDurationMillis // Show potential study time
            _elapsedTimeInFullCycleMillis.value = mTotalCycleDurationMillis // Show full progress
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        intent?.let {
            when (it.action) {
                ACTION_START -> {
                    // Read configurable parameters from intent
                    studyDurationMin = it.getIntExtra(EXTRA_STUDY_DURATION_MIN, DEFAULT_STUDY_TIME_MIN)
                    breakDurationMin = it.getIntExtra(EXTRA_BREAK_DURATION_MIN, DEFAULT_BREAK_TIME_MIN)
                    minAlarmIntervalMin = it.getIntExtra(EXTRA_MIN_ALARM_INTERVAL_MIN, DEFAULT_MIN_ALARM_INTERVAL_MIN)
                    maxAlarmIntervalMin = it.getIntExtra(EXTRA_MAX_ALARM_INTERVAL_MIN, DEFAULT_MAX_ALARM_INTERVAL_MIN)
                    showNextAlarmTimeInNotification = it.getBooleanExtra(EXTRA_SHOW_NEXT_ALARM_TIME, false)
                    alarmSoundType = it.getStringExtra(EXTRA_ALARM_SOUND_TYPE) ?: SoundOptions.DEFAULT_ALARM_SOUND_TYPE
                    eyeRestSoundType = it.getStringExtra(EXTRA_EYE_REST_SOUND_TYPE) ?: SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE
                    
                    // If EXTRA_TEST_MODE is explicitly passed in the intent, use its value.
                    // Otherwise, testMode retains its initial value (BuildConfig.DEBUG).
                    if (it.hasExtra(EXTRA_TEST_MODE)) {
                        testMode = it.getBooleanExtra(EXTRA_TEST_MODE, BuildConfig.DEBUG) // Default to BuildConfig.DEBUG if key exists but somehow fails to get bool
                    }
                    Log.d(TAG, "Starting service with study: $studyDurationMin min, break: $breakDurationMin min, testMode: $testMode (BuildConfig.DEBUG is ${BuildConfig.DEBUG})")

                    updateCurrentDurationsInternal() // Update durations based on potentially new testMode
                    
                    // 立即启动前台服务，防止 ANR
                    val initialNotification = createNotification("Starting Study Timer...")
                    startForeground(NOTIFICATION_ID, initialNotification)
                    Log.d(TAG, "Started foreground service with notification ID: $NOTIFICATION_ID")
                    
                    // Acquire wakelock only if not already held and timer is starting
                    if (wakeLock?.isHeld == false) {
                        // Calculate a suitable timeout, e.g., sum of study and break times + buffer
                        val timeout = (studyTimeMs + breakTimeMs + 60000L).let { duration -> if (duration <= 0) 3 * 60 * 60 * 1000L else duration } // Default to 3hrs if calculated is 0
                        wakeLock?.acquire(timeout)
                        Log.d(TAG, "WakeLock acquired in onStartCommand for ACTION_START with timeout ${timeout}ms")
                    }
                    startNextSessionPhase(true) // Start with study session
                }
                ACTION_STOP -> {
                    stopTimers()
                    stopForeground(STOP_FOREGROUND_REMOVE )
                    stopSelf()
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Study Timer"
            val descriptionText = "Notifications for Study Timer"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Study Timer")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startNextSessionPhase(isStudySession: Boolean) {
        Log.d(TAG, "Starting next session phase. Is Study: $isStudySession, Current State: ${_timerState.value}")
        sessionTimer?.cancel() // Cancel any existing session timer
        
        if (isStudySession) {
            _timerState.value = TimerState.STUDYING
            _timeLeftInSession.value = studyTimeMs
            _elapsedTimeInFullCycleMillis.value = 0L // Reset for new full cycle
            Log.d(TAG, "Starting study session. Duration: ${studyTimeMs}ms. Full cycle progress reset.")
            
            sessionTimer = object : CountDownTimer(studyTimeMs, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    _timeLeftInSession.value = millisUntilFinished
                    _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis - millisUntilFinished
                    updateNotification(getNotificationContentText())
                    // Handle random alarm scheduling logic
                }
                
                override fun onFinish() {
                    Log.d(TAG, "Study session finished")
                    _timeLeftInSession.value = 0L
                    _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis // Study part of cycle is complete
                    playAlarmSound() // Notify end of study
                    vibrate(VIBRATE_PATTERN_ALARM, -1)
                    // Automatically start break after study
                    startNextSessionPhase(false)
                }
            }.start()
            scheduleNextAlarm() // Schedule the first random alarm for the study session
        } else { // Start Break Session
            _timerState.value = TimerState.BREAK
            _timeLeftInSession.value = breakTimeMs
            // _elapsedTimeInFullCycleMillis continues from study session
            Log.d(TAG, "Starting break session. Duration: ${breakTimeMs}ms. Full cycle progress continues.")
            playBreakSound()
            
            sessionTimer = object : CountDownTimer(breakTimeMs, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    _timeLeftInSession.value = millisUntilFinished
                    _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis + (mBreakDurationMillis - millisUntilFinished)
                    updateNotification(getNotificationContentText())
                }
                
                override fun onFinish() {
                    Log.d(TAG, "Break session finished")
                    _timeLeftInSession.value = 0L
                    _elapsedTimeInFullCycleMillis.value = mTotalCycleDurationMillis // Full cycle is complete
                    playEyeRestCompleteSound() // Notify end of break
                    vibrate(VIBRATE_PATTERN_ALARM, -1)
                    _timerState.value = TimerState.IDLE
                    // After break, go to IDLE, user can start a new session.
                    updateNotification("Timer finished. Ready to start a new session.")
                    // Update durations and reset timeLeft for IDLE state to show potential study time
                    updateCurrentDurationsInternal()
                    _timeLeftInSession.value = mStudyDurationMillis
                    // _elapsedTimeInFullCycleMillis is already mTotalCycleDurationMillis
                    Log.d(TAG, "Timer finished. State set to IDLE.")
                    // Consider stopping the service or just wait for user action
                    // stopSelf() // if service should stop after one full cycle
                }
            }.start()
        }
    }
    
    // 保存眼部休息前的状态和时间
    private var _timeLeftBeforeEyeRest: Long = 0L
    private var _timeUntilNextAlarmBeforeEyeRest: Long = 0L
    private var _previousTimerStateBeforeEyeRest: TimerState = TimerState.IDLE
    private var _studyTimerBeforeEyeRest: CountDownTimer? = null
    private var _alarmTimerBeforeEyeRest: CountDownTimer? = null
    
    private fun startEyeRestTimer() {
        
        // 保存当前状态，以便眼部休息结束后恢复
        _previousTimerStateBeforeEyeRest = _timerState.value
        _timeLeftBeforeEyeRest = _timeLeftInSession.value
        _timeUntilNextAlarmBeforeEyeRest = _timeUntilNextAlarm.value
        
        // 不取消主计时器，让它在后台继续运行
        // 只暂存引用，不取消它们
        _studyTimerBeforeEyeRest = sessionTimer
        _alarmTimerBeforeEyeRest = alarmTimer
        
        // 切换到眼部休息状态
        _timerState.value = TimerState.EYE_REST
        _timeLeftInSession.value = EYE_REST_TIME_MS // 设置眼部休息倒计时
        
        // 眼部休息期间，_elapsedTimeInFullCycleMillis 会通过主计时器继续更新
        Log.d(TAG, "Starting eye rest. Full cycle progress: ${_elapsedTimeInFullCycleMillis.value}. Study time left: ${_timeLeftBeforeEyeRest}")

        
        updateNotification("Eye rest for 10 seconds...")
        
        playAlarmSound() // 使用闹钟声音提示眼部休息开始
        
        
        eyeRestTimer = object : CountDownTimer(EYE_REST_TIME_MS, 1000) {
            
            override fun onTick(millisUntilFinished: Long) {
                
                _timeLeftInSession.value = millisUntilFinished
                // 主计时器在后台继续运行，所以不需要在这里更新 _elapsedTimeInFullCycleMillis
                updateNotification("Eye rest: ${formatTime(millisUntilFinished)}")
            
            }
            
            
            override fun onFinish() {
                
                Log.d(TAG, "Eye rest finished. Previous state: $_previousTimerStateBeforeEyeRest")
                
                _timeLeftInSession.value = 0L // 眼部休息时间结束
                
                playEyeRestCompleteSound()
                
                vibrate(VIBRATE_PATTERN_SHORT, -1)
                
                
                // 恢复到之前的状态（应该是 STUDYING）
                _timerState.value = _previousTimerStateBeforeEyeRest
                
                
                if (_previousTimerStateBeforeEyeRest == TimerState.STUDYING) {
                    
                    // 恢复显示的剩余学习时间
                    // 注意：实际的学习计时器已经在后台继续运行，所以这里只需要恢复显示值
                    _timeLeftInSession.value = _timeLeftBeforeEyeRest - EYE_REST_TIME_MS
                    _timeUntilNextAlarm.value = _timeUntilNextAlarmBeforeEyeRest
                    
                    // 更新通知
                    updateNotification(getNotificationContentText())
                    
                    // 不需要重新启动计时器，因为它们一直在运行
                    Log.d(TAG, "Resumed study display after eye rest. Remaining: ${formatTime(_timeLeftInSession.value)}")
                    
                } else {
                    
                    // 如果之前的状态不是 STUDYING（眼部休息不应该发生在其他状态），进入 IDLE 状态
                    Log.w(TAG, "Eye rest finished, but previous state was not STUDYING ($_previousTimerStateBeforeEyeRest). Going IDLE.")
                    
                    stopTimers() 
                
                }
            
            }
        
        }.start()
    
    }

    private fun resumeStudySession(remainingMillis: Long) {
        _timerState.value = TimerState.STUDYING
        _timeLeftInSession.value = remainingMillis
        Log.d(TAG, "Resuming study session with ${remainingMillis}ms remaining. Full cycle progress: ${_elapsedTimeInFullCycleMillis.value}")

        sessionTimer?.cancel()
        sessionTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInSession.value = millisUntilFinished
                _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis - millisUntilFinished // Recalculate from full study duration
                updateNotification(getNotificationContentText())
            }

            override fun onFinish() {
                Log.d(TAG, "Resumed study session finished")
                _timeLeftInSession.value = 0L
                _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis
                playAlarmSound()
                vibrate(VIBRATE_PATTERN_ALARM, -1)
                startNextSessionPhase(false) // Start break
            }
        }.start()
        scheduleNextAlarm() // Reschedule random alarm
    }

    private fun updateCurrentDurationsInternal() {
        // These use the class members studyDurationMin, breakDurationMin, testMode
        // which are updated in onStartCommand
        mStudyDurationMillis = studyTimeMs 
        mBreakDurationMillis = breakTimeMs
        mTotalCycleDurationMillis = mStudyDurationMillis + mBreakDurationMillis
        Log.d(TAG, "Updated internal durations: Study=${mStudyDurationMillis}ms (${studyDurationMin}min), Break=${mBreakDurationMillis}ms (${breakDurationMin}min), Total=${mTotalCycleDurationMillis}ms. TestMode=$testMode")
    }
 
    private fun scheduleNextAlarm() {
        alarmTimer?.cancel()
        // Generate random interval between min and max
        val minMs = minAlarmIntervalMs
        val maxMs = maxAlarmIntervalMs
        val range = (maxMs - minMs).toInt()
        
        // Ensure range is positive
        val interval = if (range > 0) {
            minMs + random.nextInt(range)
        } else {
            minMs
        }
        
        _timeUntilNextAlarm.value = interval
        
        alarmTimer = object : CountDownTimer(interval, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeUntilNextAlarm.value = millisUntilFinished
                updateNotification()
            }
            
            override fun onFinish() {
                if (_timerState.value == TimerState.STUDYING) {
                    triggerAlarm()
                }
            }
        }.start()
    }
    
    private fun triggerAlarm() {
        // Play alarm sound
        playAlarmSound()
        
        // Vibrate device with a pattern for eye rest notification
        vibrate(VIBRATE_PATTERN_ALARM, -1)
        
        // Start eye rest timer
        startEyeRestTimer()
    }
    
    private fun startEyeRest() {
        _timerState.value = TimerState.EYE_REST
        updateNotification("Rest your eyes for 10 seconds")
        
        // Start eye rest timer
        eyeRestTimer = object : CountDownTimer(EYE_REST_TIME_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateNotification("Rest your eyes: ${formatTime(millisUntilFinished)}")
            }
            
            override fun onFinish() {
                // Play a gentle sound to indicate eye rest is complete
                playEyeRestCompleteSound()
                
                // Resume study session
                resumeStudySession()
            }
        }.start()
    }
    
    private fun resumeStudySession() {
        _timerState.value = TimerState.STUDYING
        updateNotification() // Update notification to reflect STUDYING state
        
        // Schedule next alarm (since the main timer didn't stop, we just need the next alarm)
        scheduleNextAlarm()
    }
    
    private fun startBreak() {
        Log.d(TAG, "Starting break for $breakDurationMin minutes")
        _timerState.value = TimerState.BREAK
        _timeLeftInSession.value = TimeUnit.MINUTES.toMillis(breakDurationMin.toLong()) // Use stored break duration
        
        // 播放休息开始提示音
        playBreakSound()
        
        sessionTimer?.cancel()
        sessionTimer = object : CountDownTimer(_timeLeftInSession.value, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInSession.value = millisUntilFinished
                updateNotification()
            }
            
            override fun onFinish() {
                Log.d(TAG, "Break finished.")
                // Play sound for break completion using playEyeRestCompleteSound
                Log.d(TAG, "Playing break complete sound (using eye rest sound).")
                playEyeRestCompleteSound() // Always attempt to play the sound using eyeRestSoundType

                // TODO: Decide what happens after break finishes (e.g., start new study session or stop)
                // For now, let's assume it stops or goes to a paused state, awaiting user action.
            }
        }.start()
        updateNotification("Break time! Relax for $breakDurationMin minutes.")
    }
    
    private fun playAlarmSound() {
        Log.d(TAG, "playAlarmSound: Attempting to play alarm sound. Type: $alarmSoundType")

        mediaPlayer?.release()
        mediaPlayer = null

        val soundUri = SoundOptions.getSoundUriById(this, alarmSoundType)
        if (soundUri == Uri.EMPTY) {
            Log.e(TAG, "playAlarmSound: Invalid or null sound URI for alarm type $alarmSoundType")
            return
        }
        Log.d(TAG, "playAlarmSound: Sound URI: $soundUri")

        try {
            val mp = MediaPlayer()
            this.mediaPlayer = mp 

            val headsetConnected = isHeadsetConnected()
            Log.d(TAG, "playAlarmSound: Headset connected: $headsetConnected")

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            mp.setAudioAttributes(audioAttributes)
            Log.d(TAG, "playAlarmSound: AudioAttributes set to USAGE_ALARM")

            if (headsetConnected) {
                Log.d(TAG, "playAlarmSound: Headset connected, attempting to route audio to headset.")
                if (audioManager.isBluetoothScoAvailableOffCall) {
                    Log.d(TAG, "playAlarmSound: Bluetooth SCO available. Starting SCO.")
                    audioManager.startBluetoothSco()
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "playAlarmSound: Attempted to start SCO. isBluetoothScoOn: ${audioManager.isBluetoothScoOn}")
                    }, 1000) 
                } else {
                    Log.d(TAG, "playAlarmSound: Bluetooth SCO not available/off call.")
                }
            } else {
                Log.d(TAG, "playAlarmSound: No headset detected. Playing on speaker.")
            }

            mp.setOnErrorListener { player, what, extra ->
                Log.e(TAG, "playAlarmSound: MediaPlayer Error - What: $what, Extra: $extra, URI: $soundUri")
                player.release()
                if (this.mediaPlayer == player) this.mediaPlayer = null
                true 
            }

            mp.setDataSource(this@StudyTimerService, soundUri)

            mp.setOnPreparedListener { preparedPlayer ->
                Log.d(TAG, "playAlarmSound: MediaPlayer prepared, starting playback. URI: $soundUri")
                try {
                    preparedPlayer.start()
                } catch (ise: IllegalStateException) {
                    Log.e(TAG, "playAlarmSound: MediaPlayer start failed after prepare. URI: $soundUri", ise)
                    preparedPlayer.release()
                    if (this.mediaPlayer == preparedPlayer) this.mediaPlayer = null
                }
            }

            mp.setOnCompletionListener { completedPlayer ->
                Log.d(TAG, "playAlarmSound: MediaPlayer playback completed. URI: $soundUri")
                completedPlayer.release()
                if (this.mediaPlayer == completedPlayer) this.mediaPlayer = null
                if (audioManager.isBluetoothScoOn) {
                    Log.d(TAG, "playAlarmSound: SCO was on, stopping SCO.")
                    audioManager.stopBluetoothSco()
                }
            }
            Log.d(TAG, "playAlarmSound: Preparing MediaPlayer asynchronously. URI: $soundUri")
            mp.prepareAsync()

        } catch (e: Exception) {
            Log.e(TAG, "playAlarmSound: Exception during MediaPlayer setup for URI: $soundUri", e)
            this.mediaPlayer?.release() 
            this.mediaPlayer = null
        }
    }

    private fun playBreakSound() {
        Log.d(TAG, "playBreakSound: Attempting to play break start sound using eye rest type. Type: $eyeRestSoundType")

        breakMediaPlayer?.release()
        breakMediaPlayer = null

        val soundUri = SoundOptions.getSoundUriById(this, eyeRestSoundType)
        if (soundUri == Uri.EMPTY || soundUri == null) { 
            Log.e(TAG, "playBreakSound: Invalid or null sound URI for eye rest sound type $eyeRestSoundType (used for break start)")
            return
        }
        Log.d(TAG, "playBreakSound: Sound URI: $soundUri")

        try {
            val mp = MediaPlayer()
            this.breakMediaPlayer = mp 

            val headsetConnected = isHeadsetConnected()
            Log.d(TAG, "playBreakSound: Headset connected: $headsetConnected")

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION) 
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            mp.setAudioAttributes(audioAttributes)
            Log.d(TAG, "playBreakSound: AudioAttributes set to USAGE_NOTIFICATION")

            if (headsetConnected) {
                Log.d(TAG, "playBreakSound: Headset connected, routing to headset (default behavior).")
            } else {
                Log.d(TAG, "playBreakSound: No headset, playing on speaker (default behavior).")
            }

            mp.setOnErrorListener { player, what, extra ->
                Log.e(TAG, "playBreakSound: MediaPlayer Error - What: $what, Extra: $extra, URI: $soundUri")
                player.release()
                if (this.breakMediaPlayer == player) this.breakMediaPlayer = null
                true 
            }

            mp.setDataSource(this@StudyTimerService, soundUri)

            mp.setOnPreparedListener { preparedPlayer ->
                Log.d(TAG, "playBreakSound: MediaPlayer prepared, starting playback. URI: $soundUri")
                try {
                    preparedPlayer.start()
                } catch (ise: IllegalStateException) {
                    Log.e(TAG, "playBreakSound: MediaPlayer start failed after prepare. URI: $soundUri", ise)
                    preparedPlayer.release()
                    if (this.breakMediaPlayer == preparedPlayer) this.breakMediaPlayer = null
                }
            }

            mp.setOnCompletionListener { completedPlayer ->
                Log.d(TAG, "playBreakSound: MediaPlayer playback completed. URI: $soundUri")
                completedPlayer.release()
                if (this.breakMediaPlayer == completedPlayer) this.breakMediaPlayer = null
            }

            Log.d(TAG, "playBreakSound: Preparing MediaPlayer asynchronously for URI: $soundUri")
            mp.prepareAsync()

        } catch (e: Exception) {
            Log.e(TAG, "playBreakSound: Exception during MediaPlayer setup for URI: $soundUri", e)
            this.breakMediaPlayer?.release()
            this.breakMediaPlayer = null
        }
    }


    private fun playEyeRestCompleteSound() {
        Log.d(TAG, "playEyeRestCompleteSound: Attempting to play eye rest complete sound. Type: $eyeRestSoundType")

        eyeRestMediaPlayer?.release()
        eyeRestMediaPlayer = null

        val soundUri = SoundOptions.getSoundUriById(this, eyeRestSoundType)
        if (soundUri == Uri.EMPTY || soundUri == null) { 
            Log.e(TAG, "playEyeRestCompleteSound: Invalid or null sound URI for eye rest type $eyeRestSoundType")
            return
        }
        Log.d(TAG, "playEyeRestCompleteSound: Sound URI: $soundUri")

        try {
            val mp = MediaPlayer()
            this.eyeRestMediaPlayer = mp 

            val headsetConnected = isHeadsetConnected()
            Log.d(TAG, "playEyeRestCompleteSound: Headset connected: $headsetConnected")

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            mp.setAudioAttributes(audioAttributes)
            Log.d(TAG, "playEyeRestCompleteSound: AudioAttributes set to USAGE_NOTIFICATION")

            if (headsetConnected) {
                Log.d(TAG, "playEyeRestCompleteSound: Headset connected, routing to headset (default behavior).")
            } else {
                Log.d(TAG, "playEyeRestCompleteSound: No headset, playing on speaker (default behavior).")
            }

            mp.setOnErrorListener { player, what, extra ->
                Log.e(TAG, "playEyeRestCompleteSound: MediaPlayer Error - What: $what, Extra: $extra, URI: $soundUri")
                player.release()
                if (this.eyeRestMediaPlayer == player) this.eyeRestMediaPlayer = null
                true 
            }

            mp.setDataSource(this@StudyTimerService, soundUri)

            mp.setOnPreparedListener { preparedPlayer ->
                Log.d(TAG, "playEyeRestCompleteSound: MediaPlayer prepared, starting playback. URI: $soundUri")
                try {
                    preparedPlayer.start()
                } catch (ise: IllegalStateException) {
                    Log.e(TAG, "playEyeRestCompleteSound: MediaPlayer start failed after prepare. URI: $soundUri", ise)
                    preparedPlayer.release()
                    if (this.eyeRestMediaPlayer == preparedPlayer) this.eyeRestMediaPlayer = null
                }
            }

            mp.setOnCompletionListener { completedPlayer ->
                Log.d(TAG, "playEyeRestCompleteSound: MediaPlayer playback completed. URI: $soundUri")
                completedPlayer.release()
                if (this.eyeRestMediaPlayer == completedPlayer) this.eyeRestMediaPlayer = null
            }

            Log.d(TAG, "playEyeRestCompleteSound: Preparing MediaPlayer asynchronously for URI: $soundUri")
            mp.prepareAsync()

        } catch (e: Exception) {
            Log.e(TAG, "playEyeRestCompleteSound: Exception during MediaPlayer setup for URI: $soundUri", e)
            this.eyeRestMediaPlayer?.release()
            this.eyeRestMediaPlayer = null
        }
    }

    private fun vibrate(pattern: LongArray, repeat: Int = -1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, repeat)
            }
        }
    }
    
    private fun updateNotification(customMessage: String? = null) {
        val contentText = customMessage ?: getNotificationContentText()
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun getNotificationContentText(): String {
        return when (_timerState.value) {
            TimerState.STUDYING -> {
                val sessionTime = formatTime(_timeLeftInSession.value)
                if (showNextAlarmTimeInNotification) {
                    val alarmTime = formatTime(_timeUntilNextAlarm.value)
                    "Studying: $sessionTime | Next alarm in: $alarmTime"
                } else {
                    "Studying: $sessionTime"
                }
            }
            TimerState.BREAK -> "Break Time: ${formatTime(_timeLeftInSession.value)}"
            TimerState.EYE_REST -> "Eye Rest: ${formatTime(EYE_REST_TIME_MS - (_timeUntilNextAlarm.value))}" 
            TimerState.IDLE -> "Timer Idle"
        }
    }
    
    private fun formatTime(millis: Long): String {
        return String.format(
            Locale.ENGLISH,
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) % 60,
            TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        )
    }
    
    private fun stopTimers() {
        Log.d(TAG, "Stopping all timers")
        sessionTimer?.cancel()
        sessionTimer = null
        
        alarmTimer?.cancel()
        alarmTimer = null
        
        eyeRestTimer?.cancel()
        eyeRestTimer = null
        
        _timerState.value = TimerState.IDLE
        _timeLeftInSession.value = 0
        _timeUntilNextAlarm.value = 0
        
        // Release wake lock if held
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            Log.d(TAG, "Wakelock released in stopTimers")
        }
    }
    
    private fun calculateDefaultBreak(studyDuration: Int): Int {
        val calculated = (studyDuration * (20.0 / 90.0)).roundToInt()
        return maxOf(5, calculated)
    }

    private fun startNextSessionPhase(nextState: TimerState) {
        Log.d(TAG, "startNextSessionPhase called with nextState: $nextState, current _timerState: ${_timerState.value}")
        stopTimers() // Stop any existing timers before starting new ones

        // Acquire wakelock if not already held, with a timeout relevant to the session
        if (wakeLock?.isHeld == false) {
            val sessionDurationMs = when (nextState) {
                TimerState.STUDYING -> studyTimeMs
                TimerState.BREAK -> breakTimeMs
                else -> 0L // No specific duration for IDLE or EYE_REST from here
            }
            if (sessionDurationMs > 0) {
                wakeLock?.acquire(sessionDurationMs + 10000L) // Acquire for session duration + 10s buffer
                Log.d(TAG, "Wakelock acquired for state $nextState with timeout ${sessionDurationMs + 10000L}ms")
            } else {
                // For states like EYE_REST, a shorter, fixed wakelock might be handled elsewhere if needed
                // Or a general short wakelock for other operations
                Log.d(TAG, "Wakelock not acquired for state $nextState due to zero session duration.")
            }
        }

        _timerState.value = nextState
        // Update current cycle durations based on the potentially new testMode status or settings
        when (nextState) {
            TimerState.STUDYING -> {
                _timeLeftInSession.value = studyTimeMs
                _elapsedTimeInFullCycleMillis.value = 0L // Reset for new full cycle
                Log.d(TAG, "Starting study session. Duration: ${studyTimeMs}ms. Full cycle progress reset.")
                sessionTimer = object : CountDownTimer(studyTimeMs, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        _timeLeftInSession.value = millisUntilFinished
                        _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis - millisUntilFinished
                        updateNotification(getNotificationContentText())
                        // Handle random alarm scheduling logic
                    }
                    
                    override fun onFinish() {
                        Log.d(TAG, "Study session finished")
                        _timeLeftInSession.value = 0L
                        _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis // Study part of cycle is complete
                        playAlarmSound() // Notify end of study
                        vibrate(VIBRATE_PATTERN_ALARM, -1)
                        // Automatically start break after study
                        startNextSessionPhase(false)
                    }
                }.start()
                scheduleNextAlarm() // Schedule the first random alarm for the study session
            }
            TimerState.BREAK -> {
                _timerState.value = TimerState.BREAK
                _timeLeftInSession.value = breakTimeMs
                // _elapsedTimeInFullCycleMillis continues from study session
                Log.d(TAG, "Starting break session. Duration: ${breakTimeMs}ms. Full cycle progress continues.")
                playBreakSound()
                
                sessionTimer = object : CountDownTimer(breakTimeMs, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        _timeLeftInSession.value = millisUntilFinished
                        _elapsedTimeInFullCycleMillis.value = mStudyDurationMillis + (mBreakDurationMillis - millisUntilFinished)
                        updateNotification(getNotificationContentText())
                    }
                    
                    override fun onFinish() {
                        Log.d(TAG, "Break session finished")
                        _timeLeftInSession.value = 0L
                        _elapsedTimeInFullCycleMillis.value = mTotalCycleDurationMillis // Full cycle is complete
                        playEyeRestCompleteSound() // Notify end of break
                        vibrate(VIBRATE_PATTERN_ALARM, -1)
                        _timerState.value = TimerState.IDLE
                        // After break, go to IDLE, user can start a new session.
                        updateNotification("Timer finished. Ready to start a new session.")
                        // Update durations and reset timeLeft for IDLE state to show potential study time
                        updateCurrentDurationsInternal()
                        _timeLeftInSession.value = mStudyDurationMillis
                        // _elapsedTimeInFullCycleMillis is already mTotalCycleDurationMillis
                        Log.d(TAG, "Timer finished. State set to IDLE.")
                        // Consider stopping the service or just wait for user action
                        // stopSelf() // if service should stop after one full cycle
                    }
                }.start()
            }
            else -> {
                Log.w(TAG, "startNextSessionPhase: Unexpected state $nextState. Going IDLE.")
                _timerState.value = TimerState.IDLE
            }
        }
    }

    override fun onDestroy() {
        stopTimers()
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        // Release resources
        mediaPlayer?.release()
        eyeRestMediaPlayer?.release()
        breakMediaPlayer?.release()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wakelock released")
            }
        }
        if (audioManager.isBluetoothScoOn()) { 
            Log.d(TAG, "onDestroy: Stopping Bluetooth SCO")
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        }
        stopForeground(true)
    }
}
