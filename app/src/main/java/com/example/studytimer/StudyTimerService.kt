package com.example.studytimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import java.util.concurrent.TimeUnit

class StudyTimerService : Service() {
    companion object {
        private const val TAG = "StudyTimerService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "study_timer_channel"
        
        // Default timer constants
        private const val DEFAULT_STUDY_TIME_MIN = 90 // 90 minutes
        private const val DEFAULT_BREAK_TIME_MIN = 20 // 20 minutes
        private const val DEFAULT_EYE_REST_TIME_SEC = 10 // 10 seconds
        private const val DEFAULT_MIN_ALARM_INTERVAL_MIN = 3 // 3 minutes
        private const val DEFAULT_MAX_ALARM_INTERVAL_MIN = 5 // 5 minutes
        
        // Fixed constants
        private const val EYE_REST_TIME_MS = 10 * 1000L // 10 seconds - fixed
        
        // Actions
        const val ACTION_START = "com.example.studytimer.action.START"
        const val ACTION_STOP = "com.example.studytimer.action.STOP"
        
        // Extra keys for intent parameters
        const val EXTRA_STUDY_DURATION_MIN = "com.example.studytimer.extra.STUDY_DURATION_MIN"
        const val EXTRA_MIN_ALARM_INTERVAL_MIN = "com.example.studytimer.extra.MIN_ALARM_INTERVAL_MIN"
        const val EXTRA_MAX_ALARM_INTERVAL_MIN = "com.example.studytimer.extra.MAX_ALARM_INTERVAL_MIN"
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
    
    // Timers
    private var sessionTimer: CountDownTimer? = null
    private var alarmTimer: CountDownTimer? = null
    private var eyeRestTimer: CountDownTimer? = null
    
    // Wake lock to keep CPU running
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Random for alarm intervals
    private val random = Random()
    
    // Configurable timer durations (initialized with defaults)
    private var studyDurationMin = DEFAULT_STUDY_TIME_MIN
    private var minAlarmIntervalMin = DEFAULT_MIN_ALARM_INTERVAL_MIN
    private var maxAlarmIntervalMin = DEFAULT_MAX_ALARM_INTERVAL_MIN
    
    // Calculated time values in milliseconds
    private val studyTimeMs: Long
        get() = studyDurationMin * 60 * 1000L
    
    private val breakTimeMs: Long
        get() = DEFAULT_BREAK_TIME_MIN * 60 * 1000L // Break time is fixed at 20 minutes
    
    private val minAlarmIntervalMs: Long
        get() = minAlarmIntervalMin * 60 * 1000L
    
    private val maxAlarmIntervalMs: Long
        get() = maxAlarmIntervalMin * 60 * 1000L
    
    inner class LocalBinder : Binder() {
        fun getService(): StudyTimerService = this@StudyTimerService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Create notification channel
        createNotificationChannel()
        
        // Initialize wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StudyTimer:StudyTimerWakeLock"
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START -> {
                    // Read configurable parameters from intent
                    studyDurationMin = it.getIntExtra(EXTRA_STUDY_DURATION_MIN, DEFAULT_STUDY_TIME_MIN)
                    minAlarmIntervalMin = it.getIntExtra(EXTRA_MIN_ALARM_INTERVAL_MIN, DEFAULT_MIN_ALARM_INTERVAL_MIN)
                    maxAlarmIntervalMin = it.getIntExtra(EXTRA_MAX_ALARM_INTERVAL_MIN, DEFAULT_MAX_ALARM_INTERVAL_MIN)
                    
                    // Ensure min alarm interval is less than max
                    if (minAlarmIntervalMin >= maxAlarmIntervalMin) {
                        minAlarmIntervalMin = maxAlarmIntervalMin - 1
                        if (minAlarmIntervalMin < 1) {
                            minAlarmIntervalMin = 1
                            maxAlarmIntervalMin = 2
                        }
                    }
                    
                    Log.d(TAG, "Starting with study duration: $studyDurationMin min, alarm interval: $minAlarmIntervalMin-$maxAlarmIntervalMin min")
                    
                    startForeground(NOTIFICATION_ID, createNotification("Study Timer Running"))
                    startStudySession()
                }
                ACTION_STOP -> {
                    stopTimers()
                    stopForeground(true)
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
    
    private fun createNotification(contentText: String): android.app.Notification {
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
    
    private fun startStudySession() {
        if (_timerState.value != TimerState.IDLE) {
            stopTimers()
        }
        
        // Acquire wake lock
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(studyTimeMs + 60000) // Add a minute buffer
            }
        }
        
        _timerState.value = TimerState.STUDYING
        _timeLeftInSession.value = studyTimeMs
        
        // Start session timer
        sessionTimer = object : CountDownTimer(studyTimeMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInSession.value = millisUntilFinished
                updateNotification()
            }
            
            override fun onFinish() {
                startBreak()
            }
        }.start()
        
        // Schedule first alarm
        scheduleNextAlarm()
    }
    
    private fun scheduleNextAlarm() {
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
        vibrate(pattern = longArrayOf(0, 300, 200, 300, 200, 300))
        
        // Start eye rest timer
        startEyeRest()
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
        alarmTimer?.cancel()
        
        _timerState.value = TimerState.BREAK
        _timeLeftInSession.value = breakTimeMs
        updateNotification()
        
        // Play break sound
        playBreakSound()
        
        // Start break timer
        sessionTimer = object : CountDownTimer(breakTimeMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInSession.value = millisUntilFinished
                updateNotification()
            }
            
            override fun onFinish() {
                // Start a new study session
                startStudySession()
            }
        }.start()
    }
    
    private fun playAlarmSound() {
        try {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(this@StudyTimerService, alarmSound)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { it.release() }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }
    }
    
    private fun playBreakSound() {
        try {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(this@StudyTimerService, alarmSound)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { it.release() }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing break sound", e)
        }
    }
    
    private fun playEyeRestCompleteSound() {
        try {
            // Use a gentler sound for eye rest completion
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(this@StudyTimerService, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { it.release() }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing eye rest complete sound", e)
        }
    }
    
    private fun vibrate(duration: Long = 500, pattern: LongArray? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            
            if (pattern != null) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (pattern != null) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                if (pattern != null) {
                    vibrator.vibrate(pattern, -1)
                } else {
                    vibrator.vibrate(duration)
                }
            }
        }
    }
    
    private fun updateNotification(customMessage: String? = null) {
        val contentText = customMessage ?: when (_timerState.value) {
            TimerState.STUDYING -> {
                val timeString = formatTime(_timeLeftInSession.value)
                val nextAlarmTime = formatTime(_timeUntilNextAlarm.value)
                "Studying: $timeString | Next alarm: $nextAlarmTime"
            }
            TimerState.EYE_REST -> "Rest your eyes for 10 seconds"
            TimerState.BREAK -> "Taking a break: ${formatTime(_timeLeftInSession.value)}"
            TimerState.IDLE -> "Study Timer"
        }
        
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun formatTime(millis: Long): String {
        return String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) % 60,
            TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        )
    }
    
    private fun stopTimers() {
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
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
    
    override fun onDestroy() {
        stopTimers()
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}
