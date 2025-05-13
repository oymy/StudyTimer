package com.oymyisme.studytimer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.oymyisme.studytimer.BuildConfig
import com.oymyisme.studytimer.service.StudyTimerService

/**
 * 停止计时器广播接收器
 * 
 * 接收停止计时器的广播意图，并向StudyTimerService发送停止命令
 * 遵循单一职责原则，只负责处理停止计时器的逻辑
 */
class StopTimerReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "StopTimerReceiver"
        const val ACTION_STOP_TIMER = "com.oymyisme.studytimer.action.STOP_TIMER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_TIMER) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Received stop timer broadcast")
            }
            
            // 创建停止服务的意图
            val serviceIntent = Intent(context, StudyTimerService::class.java).apply {
                action = StudyTimerService.ACTION_STOP
            }
            
            // 启动服务以执行停止操作
            context.startService(serviceIntent)
        }
    }
}
