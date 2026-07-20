package com.noirly.alarmkit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.noirly.alarmkit.AlarmKitFacade
import com.noirly.alarmkit.AlarmKitScope
import com.noirly.alarmkit.repository.AlarmRecord
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {
  private var wakeLock: PowerManager.WakeLock? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val alarmId = intent?.getStringExtra(EXTRA_ALARM_ID) ?: run {
      stopSelf()
      return START_NOT_STICKY
    }

    AlarmKitScope.scope.launch {
      val repository = AlarmKitFacade.repository(this@AlarmRingingService)
      val record = repository.getById(alarmId) ?: run {
        stopSelf()
        return@launch
      }
      acquireWakeLock()
      startForeground(NOTIFICATION_ID, buildNotification(record))
    }

    return START_STICKY
  }

  override fun onDestroy() {
    wakeLock?.let {
      if (it.isHeld) it.release()
    }
    super.onDestroy()
  }

  private fun acquireWakeLock() {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock = powerManager.newWakeLock(
      PowerManager.PARTIAL_WAKE_LOCK,
      "NoirlyAlarmKit:RingingWakeLock",
    ).apply {
      acquire(10 * 60 * 1000L)
    }
  }

  private fun buildNotification(record: AlarmRecord): Notification {
    createChannel()
    val dismissIntent = PendingIntent.getService(
      this,
      record.id.hashCode(),
      Intent(this, AlarmRingingService::class.java).apply {
        action = ACTION_DISMISS
        putExtra(EXTRA_ALARM_ID, record.id)
      },
      pendingIntentFlags(),
    )
    val snoozeIntent = PendingIntent.getService(
      this,
      record.id.hashCode() + 1,
      Intent(this, AlarmRingingService::class.java).apply {
        action = ACTION_SNOOZE
        putExtra(EXTRA_ALARM_ID, record.id)
      },
      pendingIntentFlags(),
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(record.title)
      .setContentText("Alarm ringing")
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setOngoing(true)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissIntent)
      .addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozeIntent)
      .setFullScreenIntent(dismissIntent, true)
      .build()
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Alarm Ringing",
        NotificationManager.IMPORTANCE_HIGH,
      )
      val manager = getSystemService(NotificationManager::class.java)
      manager.createNotificationChannel(channel)
    }
  }

  private fun pendingIntentFlags(): Int {
    return PendingIntent.FLAG_UPDATE_CURRENT or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
  }

  companion object {
    const val EXTRA_ALARM_ID = "alarmId"
    const val ACTION_DISMISS = "com.noirly.alarmkit.ACTION_DISMISS"
    const val ACTION_SNOOZE = "com.noirly.alarmkit.ACTION_SNOOZE"
    private const val CHANNEL_ID = "noirly_alarm_ringing"
    private const val NOTIFICATION_ID = 9100

    fun start(context: Context, alarmId: String) {
      val intent = Intent(context, AlarmRingingService::class.java).apply {
        putExtra(EXTRA_ALARM_ID, alarmId)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, AlarmRingingService::class.java))
    }
  }
}
