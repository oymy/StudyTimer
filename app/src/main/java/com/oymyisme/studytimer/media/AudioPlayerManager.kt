package com.oymyisme.studytimer.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.oymyisme.studytimer.BuildConfig

/**
 * 音频管理器类
 *
 * 负责处理所有的声音播放功能，包括闹钟声音、休息声音和眼睛休息声音
 * 使用单例模式确保整个应用只有一个 MediaPlayer 实例
 */
class AudioPlayerManager private constructor(private val context: Context) {
    companion object {
        private const val TAG = "AudioPlayerManager"

        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioPlayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 声音类型枚举
    enum class SoundType {
        ALARM, BREAK, EYE_REST
    }

    // 单一的 MediaPlayer 实例
    private var mediaPlayer: MediaPlayer? = null

    // 系统音频管理器
    private val systemAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // 音频播放最大时长（毫秒）
    private val MAX_SOUND_DURATION_MS = 5000L
    
    // 用于定时停止音频播放的Handler
    private val handler = Handler(Looper.getMainLooper())
    
    // 停止音频播放的Runnable
    private val stopSoundRunnable = Runnable {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Auto-stopping sound after ${MAX_SOUND_DURATION_MS}ms")
        }
        stopSound()
    }

    // 音频焦点变化监听器
    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 失去音频焦点，停止播放
                stopSound()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时失去音频焦点，暂停播放
                pauseSound()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 重新获得音频焦点，恢复播放
                resumeSound()
            }
        }
    }

    /**
     * 播放声音
     *
     * @param soundType 声音类型
     * @param soundUri 声音URI
     * @param loop 是否循环播放
     */
    private fun playSound(soundType: SoundType, soundUri: Uri?, loop: Boolean = false) {
        // 释放现有的 MediaPlayer
        releaseMediaPlayer()

        // 请求音频焦点
        val result = requestAudioFocus()
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Cannot get audio focus")
            }
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // 设置音频源
                if (soundUri != null) {
                    setDataSource(context, soundUri)
                } else {
                    // 使用默认声音
                    when (soundType) {
                        SoundType.ALARM -> setDataSource(context, getDefaultAlarmSound())
                        SoundType.BREAK -> setDataSource(context, getDefaultBreakSound())
                        SoundType.EYE_REST -> setDataSource(context, getDefaultEyeRestSound())
                    }
                }

                // 准备播放
                prepare()

                // 设置是否循环
                isLooping = loop

                // 设置音频路由
                routeAudioToHeadphones()

                // 开始播放
                start()
                
                // 设置定时器，确保音频播放不超过5秒
                if (!loop) { // 只对非循环播放设置定时器
                    scheduleAutoStop()
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Playing sound for type: $soundType, loop: $loop")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error playing sound", e)
            }
        }
    }

    /**
     * 停止声音播放
     */
    private fun stopSound() {
        // 移除定时器
        handler.removeCallbacks(stopSoundRunnable)
        
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
        }
        abandonAudioFocus()
    }

    /**
     * 暂停声音播放
     */
    private fun pauseSound() {
        // 移除定时器
        handler.removeCallbacks(stopSoundRunnable)
        
        mediaPlayer?.apply {
            if (isPlaying) {
                pause()
            }
        }
    }

    /**
     * 恢复声音播放
     */
    private fun resumeSound() {
        mediaPlayer?.apply {
            if (!isPlaying) {
                start()
                
                // 重新设置定时器
                if (!isLooping) { // 只对非循环播放设置定时器
                    scheduleAutoStop()
                }
            }
        }
    }

    /**
     * 释放 MediaPlayer 资源
     */
    fun releaseMediaPlayer() {
        // 移除定时器
        handler.removeCallbacks(stopSoundRunnable)
        
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        abandonAudioFocus()
    }

    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setOnAudioFocusChangeListener(afChangeListener)
                .build()
            systemAudioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            systemAudioManager.requestAudioFocus(
                afChangeListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    /**
     * 放弃音频焦点
     */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setOnAudioFocusChangeListener(afChangeListener)
                .build()
            systemAudioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            systemAudioManager.abandonAudioFocus(afChangeListener)
        }
    }

    /**
     * 将音频路由到耳机
     */
    private fun routeAudioToHeadphones() {
        val devices = systemAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var hasHeadphones = false

        for (device in devices) {
            val type = device.type
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            ) {
                hasHeadphones = true
                break
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Headphones connected: $hasHeadphones")
        }

        if (hasHeadphones) {
            // 如果连接了耳机，确保音频通过耳机播放
            systemAudioManager.mode = AudioManager.MODE_NORMAL
            systemAudioManager.isSpeakerphoneOn = false
        } else {
            // 如果没有连接耳机，使用扬声器
            systemAudioManager.mode = AudioManager.MODE_NORMAL
            systemAudioManager.isSpeakerphoneOn = true
        }
    }

    /**
     * 获取默认闹钟声音
     */
    private fun getDefaultAlarmSound(): Uri {
        return android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
    }

    /**
     * 获取默认休息声音
     */
    private fun getDefaultBreakSound(): Uri {
        return android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
    }

    /**
     * 获取默认眼睛休息声音
     */
    private fun getDefaultEyeRestSound(): Uri {
        return android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
    }

    /**
     * 播放闹钟声音
     */
    fun playAlarmSound() {
        playSound(SoundType.ALARM, getSoundUri())
    }

    /**
     * 播放眼睛休息声音
     *
     */
    fun playEyeRestSound() {
        playSound(SoundType.EYE_REST, getSoundUri())
    }

    /**
     * 播放眼睛休息完成声音
     *
     */
    fun playEyeRestCompleteSound() {
        playSound(SoundType.EYE_REST, getSoundUri())
    }

    /**
     * 根据声音类型ID获取声音URI
     *

     * @return 声音URI，如果声音类型ID无效则返回null
     */
    private fun getSoundUri(): Uri? {
        // 这里可以根据soundTypeId获取对应的声音URI
        // 目前简单返回null，使用默认声音
        return null
    }
    
    /**
     * 设置定时器自动停止音频播放
     */
    private fun scheduleAutoStop() {
        // 先移除之前的定时器，避免重复
        handler.removeCallbacks(stopSoundRunnable)
        
        // 设置新的定时器
        handler.postDelayed(stopSoundRunnable, MAX_SOUND_DURATION_MS)
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Scheduled auto-stop after ${MAX_SOUND_DURATION_MS}ms")
        }
    }
}