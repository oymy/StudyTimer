package com.oymyisme.studytimer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.oymyisme.studytimer.timer.TimerManager
import com.oymyisme.studytimer.ui.theme.StudyTimerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.core.content.edit
import androidx.compose.runtime.State

class MainActivity : ComponentActivity() {
    private var studyTimerService: StudyTimerService? = null
    private var bound = false
    
    // Service connection
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StudyTimerService.LocalBinder
            studyTimerService = binder.getService()
            bound = true
            Log.d(TAG, "Service connected")
            
            // Update UI with current service state
            lifecycleScope.launch {
                updateUIFromService()
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            studyTimerService = null
            bound = false
            Log.d(TAG, "Service disconnected")
        }
    }
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Load settings from SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _studyDurationMin.value = prefs.getInt(KEY_STUDY_DURATION, 90)
        _minAlarmIntervalMin.value = prefs.getInt(KEY_MIN_ALARM, 3)
        _maxAlarmIntervalMin.value = prefs.getInt(KEY_MAX_ALARM, 5)
        _showNextAlarmTime.value = prefs.getBoolean(KEY_SHOW_NEXT_ALARM, false)
        _alarmSoundType.value = prefs.getString(KEY_ALARM_SOUND, SoundOptions.DEFAULT_ALARM_SOUND_TYPE) ?: SoundOptions.DEFAULT_ALARM_SOUND_TYPE
        _eyeRestSoundType.value = prefs.getString(KEY_EYE_REST_SOUND, SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE) ?: SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE
        
        setContent {
            StudyTimerTheme {
                val showSettings by _showSettings.collectAsState()
                
                if (showSettings) {
                    // Show Settings Screen
                    SettingsScreen(
                        studyDurationFlow = _studyDurationMin,
                        minAlarmIntervalFlow = _minAlarmIntervalMin,
                        maxAlarmIntervalFlow = _maxAlarmIntervalMin,
                        showNextAlarmTimeFlow = _showNextAlarmTime,
                        alarmSoundTypeFlow = _alarmSoundType,
                        eyeRestSoundTypeFlow = _eyeRestSoundType,
                        onStudyDurationChange = { newDuration ->
                            // Validate: Study duration >= min/max intervals
                            val minInterval = _minAlarmIntervalMin.value
                            val maxInterval = _maxAlarmIntervalMin.value
                            if (newDuration >= minInterval && newDuration >= maxInterval) {
                                _studyDurationMin.value = newDuration
                            }
                        },
                        onMinAlarmIntervalChange = { newMinInterval ->
                            // Validate: Min interval <= max interval && Min interval <= study duration
                            val maxInterval = _maxAlarmIntervalMin.value
                            val studyDuration = _studyDurationMin.value
                            if (newMinInterval <= maxInterval && newMinInterval <= studyDuration) {
                                _minAlarmIntervalMin.value = newMinInterval
                            }
                        },
                        onMaxAlarmIntervalChange = { newMaxInterval ->
                            // Validate: Max interval >= min interval && Max interval <= study duration
                            val minInterval = _minAlarmIntervalMin.value
                            val studyDuration = _studyDurationMin.value
                            if (newMaxInterval in minInterval..studyDuration) {
                                _maxAlarmIntervalMin.value = newMaxInterval
                            }
                        },
                        onShowNextAlarmTimeChange = { show ->
                            _showNextAlarmTime.value = show
                        },
                        onAlarmSoundTypeChange = { soundType ->
                            _alarmSoundType.value = soundType
                        },
                        onEyeRestSoundTypeChange = { soundType ->
                            _eyeRestSoundType.value = soundType
                        },
                        onNavigateBack = { _showSettings.value = false }
                    )
                } else {
                    // Collect state from service if bound, otherwise use local UI state
                    val serviceTimerState = studyTimerService?.timerState?.collectAsState()
                    val serviceTimeLeftInSession: State<Long>? = studyTimerService?.timeLeftInSession?.collectAsState(0L)
                    val serviceTimeUntilNextAlarm: State<Long>? = studyTimerService?.timeUntilNextAlarm?.collectAsState(0L)
                    val serviceElapsedTimeInFullCycle: State<Long>? = studyTimerService?.elapsedTimeInFullCycleMillis?.collectAsState(0L)
                    
                    val uiTimerStateState = uiTimerState.collectAsState(TimerManager.TimerState.IDLE)
                    val uiTimeLeftInSessionState = uiTimeLeftInSession.collectAsState(0L)
                    val uiTimeUntilNextAlarmState = uiTimeUntilNextAlarm.collectAsState(0L)
                    
                    // Use service state if available, otherwise use UI state
                    val timerState = serviceTimerState?.value ?: uiTimerStateState.value
                    val timeLeftInSession = serviceTimeLeftInSession?.value ?: uiTimeLeftInSessionState.value
                    val timeUntilNextAlarm = serviceTimeUntilNextAlarm?.value ?: uiTimeUntilNextAlarmState.value
                    val elapsedTimeInFullCycle = serviceElapsedTimeInFullCycle?.value ?: 0L
                    val showNextAlarm = showNextAlarmTime.collectAsState().value
                    // Collect current settings values
                    val currentStudyDuration = _studyDurationMin.collectAsState().value
                    val currentMinInterval = _minAlarmIntervalMin.collectAsState().value
                    val currentMaxInterval = _maxAlarmIntervalMin.collectAsState().value
                    // Calculate break duration
                    val currentBreakDuration = calculateBreakDuration(currentStudyDuration)
                    
                    StudyTimerApp(
                        timerState = timerState,
                        timeLeftInSession = timeLeftInSession,
                        timeUntilNextAlarm = timeUntilNextAlarm,
                        elapsedTimeInFullCycle = elapsedTimeInFullCycle,
                        showNextAlarmTime = showNextAlarm,
                        studyDurationMin = currentStudyDuration, // Pass setting
                        minAlarmIntervalMin = currentMinInterval, // Pass setting
                        maxAlarmIntervalMin = currentMaxInterval, // Pass setting
                        breakDurationMin = currentBreakDuration, // Pass calculated break duration
                        testModeEnabled = _testModeEnabled.value, // Pass test mode state
                        cycleCompleted = cycleCompleted.collectAsState().value, // 传递周期完成状态
                        onStartClick = { startStudySession() },
                        onStopClick = { stopStudySession() },
                        onSettingsClick = { _showSettings.value = true }, // Navigate to settings
                        onTestModeToggle = { enabled ->
                            // 更新测试模式状态
                            _testModeEnabled.value = enabled
                            
                            // 直接通知服务更新测试模式状态，但不开始新的学习周期
                            if (studyTimerService != null) {
                                // 直接更新服务中的测试模式状态
                                studyTimerService?.updateTestMode(enabled, _studyDurationMin.value)
                                
                                // 如果当前是 IDLE 状态，强制更新 UI 显示
                                if (uiTimerState.value == TimerManager.TimerState.IDLE) {
                                    // 强制更新 UI 显示
                                    uiTimeLeftInSession.value = studyTimerService?.timeLeftInSession?.value ?: 0L
                                }
                            }
                            
                            // 立即显示更新结果
                            Toast.makeText(
                                this@MainActivity,
                                if (enabled) "测试模式已启用" else "测试模式已关闭",
                                Toast.LENGTH_SHORT
                            ).show()
                        }, // Handle test mode toggle
                        onContinueNextCycle = {
                            // 用户选择继续下一个周期
                            _cycleCompleted.value = false
                            studyTimerService?.resetCycleCompleted()
                            startStudySession()
                        },
                        onReturnToMain = {
                            // 用户选择返回主界面
                            _cycleCompleted.value = false
                            studyTimerService?.resetCycleCompleted()
                        }
                    )
                }
            }
        }
    }
    
    // Coroutine job references to cancel previous collectors
    private var timerStateJob: Job? = null
    private var timeLeftJob: Job? = null
    private var alarmTimeJob: Job? = null
    private var elapsedTimeInFullCycleJob: Job? = null
    private var cycleCompletedJob: Job? = null
    
    private fun updateUIFromService() {
        // Cancel any existing collectors
        timerStateJob?.cancel()
        timeLeftJob?.cancel()
        alarmTimeJob?.cancel()
        elapsedTimeInFullCycleJob?.cancel()
        cycleCompletedJob?.cancel()
        
        // Update local UI state from service when connected
        studyTimerService?.let { service ->
            timerStateJob = lifecycleScope.launch {
                service.timerState.collect { uiTimerState.value = it }
            }
            timeLeftJob = lifecycleScope.launch {
                service.timeLeftInSession.collect { uiTimeLeftInSession.value = it }
            }
            alarmTimeJob = lifecycleScope.launch {
                service.timeUntilNextAlarm.collect { uiTimeUntilNextAlarm.value = it }
            }
            elapsedTimeInFullCycleJob = lifecycleScope.launch {
                service.elapsedTimeInFullCycleMillis.collect { /* We don't have a local UI state for this, it's directly passed */ }
            }
            cycleCompletedJob = lifecycleScope.launch {
                service.cycleCompleted.collect { newValue ->
                    _cycleCompleted.value = newValue
                    // 添加调试信息，显示周期完成状态变化
                    if (newValue) {
                        Log.d(TAG, "Cycle completed state changed to TRUE in MainActivity")
                        Toast.makeText(
                            this@MainActivity,
                            "周期完成状态已更新！应该显示对话框",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    // State flows to maintain UI state when service is not bound
    private val _uiTimerState = MutableStateFlow(TimerManager.TimerState.IDLE)
    private val _uiTimeLeftInSession = MutableStateFlow(0L)
    private val _uiTimeUntilNextAlarm = MutableStateFlow(0L)
    
    // Settings state flows - needed for both service and settings screen
    private val _studyDurationMin = MutableStateFlow(90) // Default 90 minutes
    private val _minAlarmIntervalMin = MutableStateFlow(3) // Default 3 minutes
    private val _maxAlarmIntervalMin = MutableStateFlow(5) // Default 5 minutes
    private val _showNextAlarmTime = MutableStateFlow(false) // Default to false
    private val _alarmSoundType = MutableStateFlow(SoundOptions.DEFAULT_ALARM_SOUND_TYPE) // 默认闹钟提示音
    private val _eyeRestSoundType = MutableStateFlow(SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE) // 默认休息结束提示音
    
    // 测试模式状态
    private val _testModeEnabled = MutableStateFlow(true) // 默认开启测试模式，与 StudyTimerService 保持一致
    
    // 周期完成状态，用于显示结束对话框
    private val _cycleCompleted = MutableStateFlow(false)
    
    // Navigation state flow
    private val _showSettings = MutableStateFlow(false)
    
    private val uiTimerState: MutableStateFlow<TimerManager.TimerState> = _uiTimerState
    private val uiTimeLeftInSession: MutableStateFlow<Long> = _uiTimeLeftInSession
    private val uiTimeUntilNextAlarm: MutableStateFlow<Long> = _uiTimeUntilNextAlarm
    private val showNextAlarmTime: StateFlow<Boolean> = _showNextAlarmTime
    private val alarmSoundType: StateFlow<String> = _alarmSoundType
    private val eyeRestSoundType: StateFlow<String> = _eyeRestSoundType
    private val testModeEnabled: StateFlow<Boolean> = _testModeEnabled
    private val cycleCompleted: StateFlow<Boolean> = _cycleCompleted
    
    // Function to calculate break duration based on study duration
    private fun calculateBreakDuration(studyDuration: Int): Int {
        val calculated = (studyDuration * (20.0 / 90.0)).roundToInt()
        return maxOf(5, calculated) // Ensure minimum 5 minutes break
    }
    
    private fun startStudySession() {
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_START
            
            // 根据测试模式状态决定使用的参数
            val testMode = TestMode.isEnabled && _testModeEnabled.value
            
            val studyDuration = if (testMode) TestMode.getStudyDurationMin() else _studyDurationMin.value
            val breakDuration = if (testMode) TestMode.getBreakDurationMin() else calculateBreakDuration(studyDuration)
            val minAlarmInterval = if (testMode) TestMode.getMinAlarmIntervalMin() else _minAlarmIntervalMin.value
            val maxAlarmInterval = if (testMode) TestMode.getMaxAlarmIntervalMin() else _maxAlarmIntervalMin.value
            
            putExtra(StudyTimerService.EXTRA_STUDY_DURATION_MIN, studyDuration)
            putExtra(StudyTimerService.EXTRA_BREAK_DURATION_MIN, breakDuration)
            putExtra(StudyTimerService.EXTRA_MIN_ALARM_INTERVAL_MIN, minAlarmInterval)
            putExtra(StudyTimerService.EXTRA_MAX_ALARM_INTERVAL_MIN, maxAlarmInterval)
            putExtra(StudyTimerService.EXTRA_SHOW_NEXT_ALARM_TIME, _showNextAlarmTime.value)
            putExtra(StudyTimerService.EXTRA_ALARM_SOUND_TYPE, _alarmSoundType.value)
            putExtra(StudyTimerService.EXTRA_EYE_REST_SOUND_TYPE, _eyeRestSoundType.value)
            putExtra(StudyTimerService.EXTRA_TEST_MODE, testMode) // 传递测试模式状态
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        // Immediately update UI state
        _uiTimerState.value = TimerManager.TimerState.STUDYING
        _uiTimeLeftInSession.value = 90 * 60 * 1000L // Convert minutes to ms
        _uiTimeUntilNextAlarm.value = 3 * 60 * 1000L // Initial alarm time
    }
    
    private fun stopStudySession() {
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_STOP
        }
        startService(intent)
        
        // Immediately update UI state
        _uiTimerState.value = TimerManager.TimerState.IDLE
        _uiTimeLeftInSession.value = 0L
        _uiTimeUntilNextAlarm.value = 0L
    }
    
    override fun onStart() {
        super.onStart()
        // Bind to the service
        Intent(this, StudyTimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }
    
    override fun onStop() {
        super.onStop()
        // Persist settings just in case they were changed but not saved via the settings screen lambdas ( belt-and-suspenders)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(KEY_STUDY_DURATION, _studyDurationMin.value)
                .putInt(KEY_MIN_ALARM, _minAlarmIntervalMin.value)
                .putInt(KEY_MAX_ALARM, _maxAlarmIntervalMin.value)
                .putBoolean(KEY_SHOW_NEXT_ALARM, _showNextAlarmTime.value)
                .putString(KEY_ALARM_SOUND, _alarmSoundType.value)
                .putString(KEY_EYE_REST_SOUND, _eyeRestSoundType.value)
        }
        
        // Unbind from the service
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "StudyTimerPrefs"
        private const val KEY_STUDY_DURATION = "studyDuration"
        private const val KEY_MIN_ALARM = "minAlarmInterval"
        private const val KEY_MAX_ALARM = "maxAlarmInterval"
        private const val KEY_SHOW_NEXT_ALARM = "showNextAlarmTime"
        private const val KEY_ALARM_SOUND = "alarmSoundType"
        private const val KEY_EYE_REST_SOUND = "eyeRestSoundType"
    }
}