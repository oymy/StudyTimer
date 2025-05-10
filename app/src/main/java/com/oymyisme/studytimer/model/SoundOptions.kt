package com.oymyisme.studytimer.model

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

/**
 * 提示音选项数据类
 */
data class SoundOption(
    val id: String,
    val name: String,
    val uri: Uri
)

/**
 * 提供系统提示音选项的工具类
 */
object SoundOptions {
    // 默认提示音类型
    const val DEFAULT_ALARM_SOUND_TYPE = "DEFAULT_NOTIFICATION"
    const val DEFAULT_EYE_REST_SOUND_TYPE = "DEFAULT_NOTIFICATION"

    /**
     * 获取系统提示音列表
     */
    fun getSoundOptions(context: Context): List<SoundOption> {
        val options = mutableListOf<SoundOption>()
        
        // 添加默认提示音选项
        options.add(
            SoundOption(
            "DEFAULT_NOTIFICATION",
            "默认通知音",
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        )
        )
        
        options.add(
            SoundOption(
            "DEFAULT_ALARM",
            "默认闹钟音",
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        )
        )
        
        options.add(
            SoundOption(
            "DEFAULT_RINGTONE",
            "默认铃声",
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        )
        
        // 获取系统所有提示音
        val notificationManager = RingtoneManager(context)
        notificationManager.setType(RingtoneManager.TYPE_ALL)
        
        try {
            val cursor = notificationManager.cursor
            while (cursor.moveToNext()) {
                val id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX)
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = notificationManager.getRingtoneUri(cursor.position)
                
                // 避免重复添加默认提示音
                if (!options.any { it.uri == uri }) {
                    options.add(SoundOption(id, title, uri))
                }
            }
        } catch (e: Exception) {
            // 如果获取系统提示音失败，至少保留默认选项
        }
        
        return options
    }
    
    /**
     * 根据ID获取提示音URI
     */
    fun getSoundUriById(context: Context, soundId: String): Uri {
        return when (soundId) {
            "DEFAULT_NOTIFICATION" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "DEFAULT_ALARM" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            "DEFAULT_RINGTONE" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            else -> {
                // 尝试从所有提示音中查找
                val options = getSoundOptions(context)
                options.find { it.id == soundId }?.uri
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        }
    }
}
