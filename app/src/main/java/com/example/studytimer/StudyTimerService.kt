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
        
        // Timer constants
        private const val STUDY_TIME_MS = 90 * 60 * 1000L // 90 minutes
        private const val BREAK_TIME_MS = 20 * 60 * 1000L // 20 minutes
        private const val EYE_REST_TIME_MS = 10 * 1000L // 10 seconds
        private const val MIN_ALARM_INTERVAL_MS = 3 * 60 * 1000L // 3 minutes
        private const val MAX_ALARM_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        
        // Actions
        const val ACTION_START = "com.example.studytimer.action.START"
        const val ACTION_STOP = "com.example.studytimer.action.STOP"
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
                it.acquire(STUDY_TIME_MS + 60000) // Add a minute buffer
            }
        }
        
        _timerState.value = TimerState.STUDYING
        _timeLeftInSession.value = STUDY_TIME_MS
        
        // Start session timer
        sessionTimer = object : CountDownTimer(STUDY_TIME_MS, 1000) {
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
        // Generate random interval between MIN and MAX
        val interval = MIN_ALARM_INTERVAL_MS + 
                random.nextInt((MAX_ALARM_INTERVAL_MS - MIN_ALARM_INTERVAL_MS).toInt())
        
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
        
        // Vibrate device
        vibrate()
        
        // Start eye rest timer
        startEyeRest()
    }
    
    private fun startEyeRest() {
        // Pause study timer
        sessionTimer?.cancel()
        
        _timerState.value = TimerState.EYE_REST
        updateNotification()
        
        // Start eye rest timer
        eyeRestTimer = object : CountDownTimer(EYE_REST_TIME_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateNotification()
            }
            
            override fun onFinish() {
                // Resume study session
                resumeStudySession()
            }
        }.start()
    }
    
    private fun resumeStudySession() {
        _timerState.value = TimerState.STUDYING
        
        // Resume session timer with remaining time
        sessionTimer = object : CountDownTimer(_timeLeftInSession.value, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeftInSession.value = millisUntilFinished
                updateNotification()
            }
            
            override fun onFinish() {
                startBreak()
            }
        }.start()
        
        // Schedule next alarm
        scheduleNextAlarm()
    }
    
    private fun startBreak() {
        alarmTimer?.cancel()
        
        _timerState.value = TimerState.BREAK
        _timeLeftInSession.value = BREAK_TIME_MS
        updateNotification()
        
        // Play break sound
        playBreakSound()
        
        // Start break timer
        sessionTimer = object : CountDownTimer(BREAK_TIME_MS, 1000) {
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
    
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }
    
    private fun updateNotification() {
        val contentText = when (_timerState.value) {
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
