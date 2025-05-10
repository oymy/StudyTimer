package com.oymyisme.studytimer.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.oymyisme.studytimer.BuildConfig
import com.oymyisme.studytimer.ui.MainActivity
import com.oymyisme.studytimer.R
import com.oymyisme.studytimer.timer.TimerManager

/**
 * 通知管理器类
 * 
 * 负责处理所有通知相关的功能，包括创建通知渠道、更新通知等
 * 使用单例模式确保整个应用只有一个通知管理器实例
 */
class NotificationHelper private constructor(private val context: Context) {
    companion object {
        private const val TAG = "NotificationHelper"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "study_timer_channel"
        
        @Volatile
        private var INSTANCE: NotificationHelper? = null
        
        fun getInstance(context: Context): NotificationHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // 系统通知管理器
    private val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    // 通知构建器
    private var notificationBuilder: NotificationCompat.Builder? = null
    
    /**
     * 创建通知渠道
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val description = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW // 使用低重要性避免声音干扰
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                enableLights(false)
                enableVibration(false)
                setSound(null, null) // 禁用通知声音
            }
            
            systemNotificationManager.createNotificationChannel(channel)
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Notification channel created")
            }
        }
    }
    
    /**
     * 创建前台服务通知
     * 
     * @param timerState 当前计时器状态
     * @param timeLeftInSession 会话剩余时间
     * @param showNextAlarmTime 是否显示下一次闹钟时间
     * @param timeUntilNextAlarm 下一次闹钟剩余时间
     * @return 通知对象
     */
    fun createNotification(
        timerState: TimerManager.Companion.TimerState,
        timeLeftInSession: Long,
        showNextAlarmTime: Boolean,
        timeUntilNextAlarm: Long
    ): Notification {
        // 创建打开应用的 PendingIntent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建通知构建器
        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setSmallIcon(R.drawable.ic_notification)
                setContentIntent(pendingIntent)
                setOnlyAlertOnce(true)
                setSound(null) // 禁用通知声音
                setPriority(NotificationCompat.PRIORITY_LOW) // 使用低优先级
                setCategory(NotificationCompat.CATEGORY_SERVICE)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }
        }
        
        // 更新通知内容
        val title = context.getString(R.string.notification_title)
        val contentText = getNotificationContentText(timerState, timeLeftInSession, showNextAlarmTime, timeUntilNextAlarm)
        
        notificationBuilder?.apply {
            setContentTitle(title)
            setContentText(contentText)
        }
        
        return notificationBuilder?.build() ?: throw IllegalStateException("Notification builder is null")
    }
    
    /**
     * 获取通知内容文本
     */
    private fun getNotificationContentText(
        timerState: TimerManager.Companion.TimerState,
        timeLeftInSession: Long,
        showNextAlarmTime: Boolean,
        timeUntilNextAlarm: Long
    ): String {
        val timeLeftFormatted = formatTime(timeLeftInSession)
        
        return when (timerState) {
            TimerManager.Companion.TimerState.IDLE -> context.getString(R.string.notification_idle)
            TimerManager.Companion.TimerState.STUDYING -> {
                if (showNextAlarmTime) {
                    val nextAlarmFormatted = formatTime(timeUntilNextAlarm)
                    context.getString(R.string.notification_studying_with_alarm, timeLeftFormatted, nextAlarmFormatted)
                } else {
                    context.getString(R.string.notification_studying, timeLeftFormatted)
                }
            }
            TimerManager.Companion.TimerState.BREAK -> context.getString(R.string.notification_break, timeLeftFormatted)
            TimerManager.Companion.TimerState.EYE_REST -> context.getString(R.string.notification_eye_rest, timeLeftFormatted)
            else -> context.getString(R.string.notification_idle) // 默认情况
        }
    }
    
    /**
     * 更新通知
     */
    fun updateNotification(
        timerState: TimerManager.Companion.TimerState,
        timeLeftInSession: Long,
        showNextAlarmTime: Boolean,
        timeUntilNextAlarm: Long
    ) {
        val notification = createNotification(timerState, timeLeftInSession, showNextAlarmTime, timeUntilNextAlarm)
        systemNotificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 格式化时间
     */
    private fun formatTime(timeMillis: Long): String {
        val minutes = timeMillis / 60000
        val seconds = (timeMillis % 60000) / 1000
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        systemNotificationManager.cancelAll()
    }
}
