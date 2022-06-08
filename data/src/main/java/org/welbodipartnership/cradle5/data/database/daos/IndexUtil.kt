package org.welbodipartnership.cradle5.data.database.daos

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import org.acra.ktx.sendSilentlyWithAcra
import org.welbodipartnership.cradle5.data.database.entities.AppEntity

private const val TAG = "IndexUtil"

// Apparently, some devices' SQLite versions do not support ROW_NUMBER at all
private var isFailingRawQueryDueToOutdatedSqlite = false

internal inline fun <reified T : AppEntity> getRowNumberSafe(
  entity: T,
  countTotalBlock: () -> Int,
  createRawQueryBlock: () -> SimpleSQLiteQuery,
  runRawQueryBlock: (SimpleSQLiteQuery) -> Int?,
  getAllEntitiesBlock: () -> List<T>,
  crossinline allEntitiesComparator: (T, T) -> Int = { a, b -> a.id.compareTo(b.id) }
): Int? {
  when (countTotalBlock()) {
    0 -> {
      Log.d(TAG, "no ${entity::class.simpleName}s; using null")
      return null
    }
    1 -> {
      Log.d(TAG, "only one ${entity::class.simpleName}; using index of 0")
      return 0
    }
  }

  return if (isFailingRawQueryDueToOutdatedSqlite) {
    getRowNumByWorkaround(getAllEntitiesBlock, allEntitiesComparator, entity)
  } else {
    try {
      // Room doesn't support this type of query
      val query = createRawQueryBlock()
      // ROW_NUMBER is 1-based
      runRawQueryBlock(query)?.let { (it - 1).coerceAtLeast(0) }
    } catch (e: SQLiteException) {
      isFailingRawQueryDueToOutdatedSqlite = true
      e.sendSilentlyWithAcra()
      getRowNumByWorkaround(getAllEntitiesBlock, allEntitiesComparator, entity)
    } catch (e: Throwable) {
      // Strange exception from Google Play console from an Infinix HOT 10 Play device running Android
      // 10 (SDK 29). The exception came from a native method
      // (android.database.sqlite.SQLiteConnection.nativePrepareStatement). It might be due to R8
      // optimizing out an exception type that was thrown only from native code; R8 wouldn't have
      // detected it as a usage.
      if (
        e.stackTraceToString().contains("exception.class.missing._Unknown_") ||
        e.toString().contains("exception.class.missing._Unknown_")
      ) {
        isFailingRawQueryDueToOutdatedSqlite = true
        Log.e(TAG, "Got missing class exception, falling back", e)
        e.sendSilentlyWithAcra()
        getRowNumByWorkaround(getAllEntitiesBlock, allEntitiesComparator, entity)
      } else {
        throw e
      }
    }
  }
}

private inline fun <reified T : AppEntity> getRowNumByWorkaround(
  getAllEntitiesBlock: () -> List<T>,
  crossinline allEntitiesComparator: (T, T) -> Int,
  entity: T
): Int? {
  Log.e(TAG, "Failed to run ROW_NUMBER statement for ${T::class.simpleName}; falling back to getting all and sort")

  getAllEntitiesBlock()
    .sortedWith { a, b -> allEntitiesComparator(a, b) }
    .forEachIndexed { index: Int, current: T ->
      if (current.id == entity.id) {
        return index
      }
    }

  Log.w(TAG, "Could not find entity with primary key ${entity.id}")
  return null
}
