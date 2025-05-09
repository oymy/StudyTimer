package com.oymyisme.studytimer

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues.TAG
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.oymyisme.model.TimerSettings
import com.oymyisme.model.TimerState
import com.oymyisme.studytimer.settings.ThemeMode
import com.oymyisme.studytimer.settings.ThemeSettings
import com.oymyisme.studytimer.settings.ThemeSettingsScreen
import com.oymyisme.studytimer.timer.TimerManager
import com.oymyisme.studytimer.ui.theme.StudyTimerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt
import androidx.core.content.edit
import androidx.compose.runtime.State

class MainActivity : ComponentActivity() {
    private var studyTimerService: StudyTimerService? = null
    private var bound = false
    
    // 主题设置管理器
    private lateinit var themeSettings: ThemeSettings
    
    // 计时器设置状态流
    private val _timerSettings = MutableStateFlow(TimerSettings())
    val timerSettings: StateFlow<TimerSettings> = _timerSettings
    
    // 计时器状态流
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState
    
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
    
    /**
     * 更新计时器设置
     */
    fun updateSettings(update: (TimerSettings) -> TimerSettings) {
        _timerSettings.value = update(_timerSettings.value)
        // 保存到 SharedPreferences
        saveSettings()
    }
    
    /**
     * 从 SharedPreferences 加载设置
     */
    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _timerSettings.value = TimerSettings(
            studyDurationMin = prefs.getInt(KEY_STUDY_DURATION, 90),
            minAlarmIntervalMin = prefs.getInt(KEY_MIN_ALARM, 3),
            maxAlarmIntervalMin = prefs.getInt(KEY_MAX_ALARM, 5),
            showNextAlarmTime = prefs.getBoolean(KEY_SHOW_NEXT_ALARM, false),
            alarmSoundType = prefs.getString(KEY_ALARM_SOUND, SoundOptions.DEFAULT_ALARM_SOUND_TYPE)
                ?: SoundOptions.DEFAULT_ALARM_SOUND_TYPE,
            eyeRestSoundType = prefs.getString(KEY_EYE_REST_SOUND, SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE)
                ?: SoundOptions.DEFAULT_EYE_REST_SOUND_TYPE,
            testModeEnabled = prefs.getBoolean(KEY_TEST_MODE, false)
        )
    }
    
    /**
     * 验证设置的有效性
     * 确保学习时间大于或等于最小/最大间隔，以及最小间隔小于或等于最大间隔
     */
    private fun isValidSettings(settings: TimerSettings): Boolean {
        val studyDuration = settings.studyDurationMin
        val minInterval = settings.minAlarmIntervalMin
        val maxInterval = settings.maxAlarmIntervalMin
        
        // 验证学习时间大于或等于最小/最大间隔
        if (studyDuration < minInterval || studyDuration < maxInterval) {
            Toast.makeText(
                this,
                "学习时间必须大于或等于闹钟间隔",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        
        // 验证最小间隔小于或等于最大间隔
        if (minInterval > maxInterval) {
            Toast.makeText(
                this,
                "最小闹钟间隔必须小于或等于最大闹钟间隔",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        
        return true
    }
    
    /**
     * 保存设置到 SharedPreferences
     */
    private fun saveSettings() {
        val settings = _timerSettings.value
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_STUDY_DURATION, settings.studyDurationMin)
            putInt(KEY_MIN_ALARM, settings.minAlarmIntervalMin)
            putInt(KEY_MAX_ALARM, settings.maxAlarmIntervalMin)
            putBoolean(KEY_SHOW_NEXT_ALARM, settings.showNextAlarmTime)
            putString(KEY_ALARM_SOUND, settings.alarmSoundType)
            putString(KEY_EYE_REST_SOUND, settings.eyeRestSoundType)
            putBoolean(KEY_TEST_MODE, settings.testModeEnabled)
        }
    }
    
    /**
     * 更新计时器状态
     */
    private fun updateTimerState(update: (TimerState) -> TimerState) {
        _timerState.value = update(_timerState.value)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化主题设置管理器
        themeSettings = ThemeSettings(this)

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

        // 加载设置
        loadSettings()

        setContent {
            // 收集主题设置状态
            val themeMode by themeSettings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColorEnabled by themeSettings.dynamicColor.collectAsState(initial = true)
            val amoledBlackEnabled by themeSettings.amoledBlack.collectAsState(initial = false)

            // 根据主题设置决定暗色模式
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            StudyTimerTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColorEnabled,
                amoledBlack = amoledBlackEnabled
            ) {
                val showSettings by _showSettings.collectAsState()
                val showThemeSettings by _showThemeSettings.collectAsState()

                when {
                    showThemeSettings -> {
                        // 显示主题设置界面
                        ThemeSettingsScreen(
                            themeSettings = themeSettings,
                            onNavigateBack = { _showThemeSettings.value = false }
                        )
                    }

                    showSettings -> {
                        // 显示常规设置界面
                        val currentSettings = _timerSettings.collectAsState().value
                        
                        SettingsScreen(
                            timerSettings = _timerSettings,
                            onNavigateToThemeSettings = { _showThemeSettings.value = true },
                            onSettingsChange = { updatedSettings ->
                                // 验证设置的有效性
                                if (isValidSettings(updatedSettings)) {
                                    updateSettings { updatedSettings }
                                }
                            },
                            onNavigateBack = { _showSettings.value = false }
                        )
                    }
                    
                    else -> {
                        // 收集当前的计时器状态和设置
                        val currentTimerState = _timerState.collectAsState().value
                        val currentSettings = _timerSettings.collectAsState().value
                        
                        // 直接使用TimerSettings中的breakDurationMin属性，提高代码内聚性
                        val breakDurationMin = currentSettings.breakDurationMin
                        
                        // 直接使用数据类传递给UI组件，提高代码可维护性
                        StudyTimerApp(
                            timerState = currentTimerState,
                            settings = currentSettings,
                            testModeChangeTrigger = _testModeChangeTrigger.collectAsState().value,
                            onStartClick = { startStudySession() },
                            onStopClick = { stopStudySession() },
                            onSettingsClick = {
                                _showSettings.value = true
                            },
                            onTestModeToggle = { enabled ->
                                // 更新测试模式状态
                                updateSettings { it.copy(testModeEnabled = enabled) }
                                
                                // 更新TestMode类中的isEnabled属性
                                TestMode.isEnabled = enabled
                                
                                // 更新触发器值，强制UI重组
                                _testModeChangeTrigger.value = System.currentTimeMillis().toString()
                                
                                // 直接通知服务更新测试模式状态
                                studyTimerService?.let { service ->
                                    service.updateTestMode(enabled, currentSettings.studyDurationMin)
                                    
                                    // 如果当前是IDLE状态，强制更新UI显示
                                    if (currentTimerState.timerState == TimerManager.TimerState.IDLE) {
                                        updateTimerState { it.copy(
                                            timeLeftInSession = service.timeLeftInSession.value ?: 0L
                                        )}
                                    }
                                }
                                
                                // 立即显示更新结果
                                Toast.makeText(
                                    this@MainActivity,
                                    if (enabled) "测试模式已启用" else "测试模式已关闭",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onContinueNextCycle = {
                                // 用户选择继续下一个周期
                                updateTimerState { it.copy(cycleCompleted = false) }
                                studyTimerService?.resetCycleCompleted()
                                startStudySession()
                            },
                            onReturnToMain = {
                                // 用户选择返回主界面
                                updateTimerState { it.copy(cycleCompleted = false) }
                                studyTimerService?.resetCycleCompleted()
                            }
                        )
                    }
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
            // 使用单一协程收集所有状态并更新到新的数据结构
            timerStateJob = lifecycleScope.launch {
                service.timerState.collect { timerStateValue ->
                    updateTimerState { currentState ->
                        currentState.copy(timerState = timerStateValue)
                    }
                }
            }
            
            timeLeftJob = lifecycleScope.launch {
                service.timeLeftInSession.collect { timeLeftValue ->
                    updateTimerState { currentState ->
                        currentState.copy(timeLeftInSession = timeLeftValue)
                    }
                }
            }
            
            alarmTimeJob = lifecycleScope.launch {
                service.timeUntilNextAlarm.collect { alarmTimeValue ->
                    updateTimerState { currentState ->
                        currentState.copy(timeUntilNextAlarm = alarmTimeValue)
                    }
                }
            }
            
            elapsedTimeInFullCycleJob = lifecycleScope.launch {
                service.elapsedTimeInFullCycleMillis.collect { elapsedTimeValue ->
                    updateTimerState { currentState ->
                        currentState.copy(elapsedTimeInFullCycle = elapsedTimeValue)
                    }
                }
            }
            
            cycleCompletedJob = lifecycleScope.launch {
                service.cycleCompleted.collect { cycleCompletedValue ->
                    updateTimerState { currentState ->
                        currentState.copy(cycleCompleted = cycleCompletedValue)
                    }
                    
                    // 周期完成状态变化时的处理现在由UI组件自动处理
                    // 符合高内聚、低耦合的设计原则
                }
            }
        }
    }
    
    // 测试模式变化触发器，用于强制 UI 重组
    private val _testModeChangeTrigger = MutableStateFlow("")
    
    // 导航状态流
    private val _showSettings = MutableStateFlow(false)
    private val _showThemeSettings = MutableStateFlow(false)
    
    // 通过TimerSettings和TimerState数据类提供状态流，不再需要单独的状态流变量
    
    // 休息时间计算现在已移到 TimerSettings 数据类中，遵循高内聚、低耦合的设计原则
    
    private fun startStudySession() {
        val currentSettings = _timerSettings.value
        
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_START
            
            // 根据测试模式状态决定使用的参数
            val testMode = TestMode.isEnabled && currentSettings.testModeEnabled
            
            // 根据测试模式状态选择合适的设置
            val timerSettings = if (testMode) {
                TestMode.createTestModeSettings()
            } else {
                currentSettings
            }
            
            // 直接使用 TimerSettings 中的属性，避免重复计算
            val studyDuration = timerSettings.studyDurationMin
            val breakDuration = timerSettings.breakDurationMin // 使用数据类中的属性，遵循高内聚原则
            val minAlarmInterval = timerSettings.minAlarmIntervalMin
            val maxAlarmInterval = timerSettings.maxAlarmIntervalMin
            
            putExtra(StudyTimerService.EXTRA_STUDY_DURATION_MIN, studyDuration)
            putExtra(StudyTimerService.EXTRA_BREAK_DURATION_MIN, breakDuration)
            putExtra(StudyTimerService.EXTRA_MIN_ALARM_INTERVAL_MIN, minAlarmInterval)
            putExtra(StudyTimerService.EXTRA_MAX_ALARM_INTERVAL_MIN, maxAlarmInterval)
            putExtra(StudyTimerService.EXTRA_SHOW_NEXT_ALARM_TIME, timerSettings.showNextAlarmTime) // 使用选定的设置
            putExtra(StudyTimerService.EXTRA_ALARM_SOUND_TYPE, timerSettings.alarmSoundType) // 使用选定的设置
            putExtra(StudyTimerService.EXTRA_EYE_REST_SOUND_TYPE, timerSettings.eyeRestSoundType) // 使用选定的设置
            putExtra(StudyTimerService.EXTRA_TEST_MODE, testMode) // 传递测试模式状态
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        // 立即更新UI状态
        updateTimerState { currentState ->
            currentState.copy(
                timerState = TimerManager.TimerState.STUDYING,
                timeLeftInSession = currentSettings.studyDurationMin * 60 * 1000L, // 将分钟转换为毫秒
                timeUntilNextAlarm = currentSettings.minAlarmIntervalMin * 60 * 1000L // 初始闹钟时间
            )
        }
    }
    
    private fun stopStudySession() {
        val intent = Intent(this, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_STOP
        }
        startService(intent)
        
        // 立即更新UI状态
        updateTimerState { currentState ->
            currentState.copy(
                timerState = TimerManager.TimerState.IDLE,
                timeLeftInSession = 0L,
                timeUntilNextAlarm = 0L
            )
        }
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
        // 保存设置，以防设置已更改但未通过设置界面保存
        saveSettings()
        
        // 解绑服务
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
        private const val KEY_TEST_MODE = "testModeEnabled"
    }
}