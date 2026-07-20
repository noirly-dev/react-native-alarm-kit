package com.noirly.alarmkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Application-scoped coroutine scope shared across alarm components. */
object AlarmKitScope {
  val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
