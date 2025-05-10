package com.oymyisme.studytimer.media

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.oymyisme.studytimer.BuildConfig

/**
 * 振动管理器类
 * 
 * 负责处理所有振动相关的功能
 * 使用单例模式确保整个应用只有一个振动管理器实例
 */
class VibrationManager private constructor(private val context: Context) {
    companion object {
        private const val TAG = "VibrationManager"
        
        // 振动模式
        val VIBRATE_PATTERN_ALARM = longArrayOf(0, 1000, 500, 1000) // 等待0毫秒，振动1秒，暂停0.5秒，振动1秒
        val VIBRATE_PATTERN_SHORT = longArrayOf(0, 300)            // 等待0毫秒，振动0.3秒
        
        @Volatile
        private var INSTANCE: VibrationManager? = null
        
        fun getInstance(context: Context): VibrationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VibrationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // 振动器
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    /**
     * 执行振动
     * 
     * @param pattern 振动模式
     * @param repeat 重复索引，-1表示不重复
     */
    fun vibrate(pattern: LongArray, repeat: Int = -1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, repeat)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, repeat)
        }
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Vibrating with pattern: ${pattern.contentToString()}, repeat: $repeat")
        }
    }
    
    /**
     * 执行闹钟振动
     */
    fun vibrateAlarm() {
        vibrate(VIBRATE_PATTERN_ALARM)
    }
    
    /**
     * 执行闹钟振动（别名，与 TimerManager 回调接口保持一致）
     */
    fun vibrateForAlarm() {
        vibrateAlarm()
    }
    
    /**
     * 执行短振动
     */
    fun vibrateShort() {
        vibrate(VIBRATE_PATTERN_SHORT)
    }
    
    /**
     * 取消振动
     */
    fun cancelVibration() {
        vibrator.cancel()
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Vibration cancelled")
        }
    }
    
    /**
     * 检查设备是否支持振动
     */
    fun hasVibrator(): Boolean {
        return vibrator.hasVibrator()
    }
}
