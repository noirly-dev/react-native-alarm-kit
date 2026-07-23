package com.noirly.alarmkit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.noirly.alarmkit.AlarmEngine
import com.noirly.alarmkit.AlarmKitFacade
import com.noirly.alarmkit.AlarmKitScope
import com.noirly.alarmkit.repository.AlarmRecord
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {
  private var wakeLock: PowerManager.WakeLock? = null
  private var mediaPlayer: MediaPlayer? = null
  private var vibrator: Vibrator? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val alarmId = intent?.getStringExtra(EXTRA_ALARM_ID) ?: run {
      stopSelf()
      return START_NOT_STICKY
    }

    when (intent.action) {
      ACTION_DISMISS -> {
        stopAlarmFeedback()
        AlarmKitFacade.initialize(this)
        AlarmKitScope.scope.launch {
          try {
            AlarmEngine(this@AlarmRingingService).dismissAlarm(alarmId)
          } catch (_: Exception) {
            stopSelf()
          }
        }
        return START_NOT_STICKY
      }
      ACTION_SNOOZE -> {
        stopAlarmFeedback()
        AlarmKitFacade.initialize(this)
        AlarmKitScope.scope.launch {
          try {
            AlarmEngine(this@AlarmRingingService).snoozeAlarm(alarmId, null)
          } catch (_: Exception) {
            stopSelf()
          }
        }
        return START_NOT_STICKY
      }
    }

    AlarmKitScope.scope.launch {
      val repository = AlarmKitFacade.repository(this@AlarmRingingService)
      val record = repository.getById(alarmId) ?: run {
        stopSelf()
        return@launch
      }
      acquireWakeLock()
      startForeground(NOTIFICATION_ID, buildNotification(record))
      startAlarmFeedback()
    }

    return START_STICKY
  }

  override fun onDestroy() {
    stopAlarmFeedback()
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

  private fun startAlarmFeedback() {
    stopAlarmFeedback()
    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      ?: return

    try {
      mediaPlayer = MediaPlayer().apply {
        setDataSource(this@AlarmRingingService, uri)
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build(),
        )
        isLooping = true
        prepare()
        start()
      }
    } catch (_: Exception) {
      mediaPlayer?.release()
      mediaPlayer = null
    }

    vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = getSystemService(VibratorManager::class.java)
      manager.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    val pattern = longArrayOf(0, 800, 400, 800, 400)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    } else {
      @Suppress("DEPRECATION")
      vibrator?.vibrate(pattern, 0)
    }
  }

  private fun stopAlarmFeedback() {
    try {
      mediaPlayer?.stop()
    } catch (_: Exception) {
      // already stopped
    }
    mediaPlayer?.release()
    mediaPlayer = null
    vibrator?.cancel()
    vibrator = null
  }

  private fun buildNotification(record: AlarmRecord): Notification {
    createChannel()
    val launchIntent = createLaunchPendingIntent(record.id)
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
      .setContentIntent(launchIntent)
      .setFullScreenIntent(launchIntent, true)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissIntent)
      .addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozeIntent)
      .build()
  }

  private fun createLaunchPendingIntent(alarmId: String): PendingIntent {
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
      addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
          Intent.FLAG_ACTIVITY_SINGLE_TOP or
          Intent.FLAG_ACTIVITY_CLEAR_TOP,
      )
      putExtra(EXTRA_ALARM_ID, alarmId)
    } ?: Intent().apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      putExtra(EXTRA_ALARM_ID, alarmId)
    }
    return PendingIntent.getActivity(
      this,
      alarmId.hashCode() + 2,
      launchIntent,
      pendingIntentFlags(),
    )
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
      val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Alarm Ringing",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Full-screen alarm alerts"
        setBypassDnd(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        enableVibration(true)
        setSound(alarmUri, attrs)
      }
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
    /** Bumped so channel sound/vibration settings apply on devices that already had v1. */
    private const val CHANNEL_ID = "noirly_alarm_ringing_v2"
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
