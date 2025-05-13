package com.oymyisme.studytimer

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.oymyisme.studytimer.service.StudyTimerService
import kotlin.system.exitProcess

/**
 * 应用程序类
 * 
 * 负责全局应用程序状态管理，包括生命周期监听和崩溃处理
 * 遵循单一职责原则，只负责应用级别的管理
 */
class StudyTimerApplication : Application(), DefaultLifecycleObserver, Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val TAG = "StudyTimerApplication"
    }
    
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null
    
    override fun onCreate() {
        super<Application>.onCreate()
        
        // 注册生命周期观察者
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // 设置全局异常处理器
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application created")
        }
    }
    
    /**
     * 应用进入后台时调用
     * 在这里我们不做任何操作，因为我们希望服务继续在后台运行
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application entered background")
        }
    }
    
    /**
     * 应用被销毁时调用
     * 确保释放所有资源
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application destroyed")
        }
        
        // 停止服务
        cleanupResources()
    }
    
    /**
     * 处理未捕获的异常
     * 在应用崩溃前确保释放资源
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "Uncaught exception", throwable)
        }
        
        try {
            // 清理资源
            cleanupResources()
            
            // 等待资源释放
            Thread.sleep(200)
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        } finally {
            // 调用默认的异常处理器
            defaultExceptionHandler?.uncaughtException(thread, throwable)
            
            // 确保进程终止
            exitProcess(1)
        }
    }
    
    /**
     * 清理资源
     * 停止服务并释放资源
     */
    private fun cleanupResources() {
        try {
            // 停止计时器服务
            val intent = Intent(this, StudyTimerService::class.java).apply {
                action = StudyTimerService.ACTION_STOP
            }
            startService(intent)
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Resources cleaned up")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service", e)
        }
    }
}
