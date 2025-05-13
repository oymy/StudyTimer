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
import com.oymyisme.studytimer.receiver.StopTimerReceiver
import com.oymyisme.studytimer.BuildConfig
import com.oymyisme.studytimer.R
import com.oymyisme.studytimer.model.TimerPhase
import com.oymyisme.studytimer.timer.TimerManager
import com.oymyisme.studytimer.ui.MainActivity
import java.util.Locale

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
            val importance = NotificationManager.IMPORTANCE_HIGH // 使用高重要性确保在锁屏上显示
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                enableLights(true) // 启用指示灯
                enableVibration(false)
                setSound(null, null) // 禁用通知声音
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // 在锁屏上完全显示通知
                setShowBadge(true) // 显示通知徽章
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
     * @param timerPhase 当前计时器状态
     * @param timeLeftInSession 会话剩余时间
     * @param showNextAlarmTime 是否显示下一次闹钟时间
     * @param timeUntilNextAlarm 下一次闹钟剩余时间
     * @return 通知对象
     */
    fun createNotification(
        timerPhase: TimerPhase,
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
                setPriority(NotificationCompat.PRIORITY_MAX) // 使用最高优先级
                setCategory(NotificationCompat.CATEGORY_ALARM) // 使用闹钟类别
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 在锁屏上完全显示通知
                // 添加全屏意图，确保在锁屏状态下显示
                setFullScreenIntent(pendingIntent, true)
            }
        }
        
        // 创建停止按钮的PendingIntent
        val stopIntent = Intent(context, StopTimerReceiver::class.java).apply {
            action = StopTimerReceiver.ACTION_STOP_TIMER
        }
        
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 更新通知内容
        val title = context.getString(R.string.notification_title)
        val contentText = getNotificationContentText(timerPhase, timeLeftInSession, showNextAlarmTime, timeUntilNextAlarm)
        
        notificationBuilder?.apply {
            setContentTitle(title)
            setContentText(contentText)
            
            // 清除之前的所有按钮
            clearActions()
            
            // 只在非空闲状态下显示停止按钮
            if (timerPhase != TimerPhase.IDLE) {
                addAction(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_media_pause,
                        context.getString(R.string.notification_action_stop),
                        stopPendingIntent
                    ).build()
                )
            }
        }
        
        return notificationBuilder?.build() ?: throw IllegalStateException("Notification builder is null")
    }
    
    /**
     * 获取通知内容文本
     */
    private fun getNotificationContentText(
        timerPhase: TimerPhase,
        timeLeftInSession: Long,
        showNextAlarmTime: Boolean,
        timeUntilNextAlarm: Long
    ): String {
        val timeLeftFormatted = formatTime(timeLeftInSession)
        
        return when (timerPhase) {
            TimerPhase.IDLE -> context.getString(R.string.notification_idle)
            TimerPhase.STUDYING -> {
                if (showNextAlarmTime) {
                    val nextAlarmFormatted = formatTime(timeUntilNextAlarm)
                    context.getString(R.string.notification_studying_with_alarm, timeLeftFormatted, nextAlarmFormatted)
                } else {
                    context.getString(R.string.notification_studying, timeLeftFormatted)
                }
            }
            TimerPhase.BREAK -> context.getString(R.string.notification_break, timeLeftFormatted)
            TimerPhase.EYE_REST -> context.getString(R.string.notification_eye_rest, timeLeftFormatted)
            else -> context.getString(R.string.notification_idle) // 默认情况
        }
    }
    
    /**
     * 更新通知
     */
    fun updateNotification(
        timerPhase: TimerPhase,
        timeLeftInSession: Long,
        showNextAlarmTime: Boolean,
        timeUntilNextAlarm: Long
    ) {
        val notification = createNotification(timerPhase, timeLeftInSession, showNextAlarmTime, timeUntilNextAlarm)
        systemNotificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 格式化时间
     */
    private fun formatTime(timeMillis: Long): String {
        val minutes = timeMillis / 60000
        val seconds = (timeMillis % 60000) / 1000
        return String.format(Locale.ENGLISH,"%d:%02d", minutes, seconds)
    }

}
