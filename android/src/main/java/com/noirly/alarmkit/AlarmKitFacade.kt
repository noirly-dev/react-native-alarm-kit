package com.noirly.alarmkit

import android.content.Context
import com.noirly.alarmkit.events.AlarmEventEmitter
import com.noirly.alarmkit.repository.AlarmRepository
import com.noirly.alarmkit.scheduler.AlarmScheduler

/** Application-scoped facade shared by TurboModule and broadcast receivers. */
object AlarmKitFacade {
  @Volatile
  private var repository: AlarmRepository? = null
  @Volatile
  private var scheduler: AlarmScheduler? = null
  @Volatile
  private var eventEmitter: AlarmEventEmitter? = null

  fun initialize(context: Context) {
    val appContext = context.applicationContext
    if (repository == null) {
      synchronized(this) {
        if (repository == null) {
          repository = AlarmRepository(appContext)
          scheduler = AlarmScheduler.create(appContext)
          eventEmitter = AlarmEventEmitter(null)
        }
      }
    }
  }

  fun repository(context: Context): AlarmRepository {
    initialize(context)
    return repository!!
  }

  fun scheduler(context: Context): AlarmScheduler {
    initialize(context)
    return scheduler!!
  }

  fun eventEmitter(context: Context): AlarmEventEmitter {
    initialize(context)
    return eventEmitter!!
  }
}
