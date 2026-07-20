package com.noirly.alarmkit.repository

import android.content.Context
import com.noirly.alarmkit.AlarmKitException

class AlarmRepository(context: Context) {
  private val dao = AlarmDatabase.getInstance(context).alarmDao()

  suspend fun insert(record: AlarmRecord) {
    dao.insert(AlarmMapper.recordToEntity(record))
  }

  suspend fun update(record: AlarmRecord) {
    dao.update(AlarmMapper.recordToEntity(record))
  }

  suspend fun getById(id: String): AlarmRecord? {
    return dao.getById(id)?.let { AlarmMapper.entityToRecord(it) }
  }

  suspend fun requireById(id: String): AlarmRecord {
    return getById(id) ?: throw AlarmKitException.AlarmNotFound(id)
  }

  suspend fun getAll(): List<AlarmRecord> {
    return dao.getAll().map { AlarmMapper.entityToRecord(it) }
  }

  suspend fun deleteById(id: String) {
    dao.deleteById(id)
  }

  suspend fun deleteAll() {
    dao.deleteAll()
  }

  suspend fun count(): Int = dao.count()
}
