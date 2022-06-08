package org.welbodipartnership.cradle5.data.database.daos

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import org.welbodipartnership.cradle5.data.database.entities.AppEntity
import org.welbodipartnership.cradle5.data.database.entities.CradleTrainingForm
import org.welbodipartnership.cradle5.data.database.entities.District
import org.welbodipartnership.cradle5.data.database.entities.Facility
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import java.lang.ClassCastException

private const val TAG = "IndexUtil"

internal suspend inline fun <reified T : AppEntity> getRowNumberSafe(
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

  return try {
    // Room doesn't support this type of query
    val query = createRawQueryBlock()
    // ROW_NUMBER is 1-based
    runRawQueryBlock(query)?.let { (it - 1).coerceAtLeast(0) }
  } catch (e: SQLiteException) {
    getRowNumByWorkaround(getAllEntitiesBlock, allEntitiesComparator, entity)
  } catch (e: Throwable) {
    // Strange exception from Google Play console from an Infinix HOT 10 Play device running Android
    // 10 (SDK 29). The exception came from a native method
    // (android.database.sqlite.SQLiteConnection.nativePrepareStatement).
    if (
      e.stackTraceToString().contains("exception.class.missing._Unknown_") ||
      e.toString().contains("exception.class.missing._Unknown_")
    ) {
      Log.e(TAG, "Got missing class exception, falling back", e)
      getRowNumByWorkaround(getAllEntitiesBlock, allEntitiesComparator, entity)
    } else {
      throw e
    }
  }
}

private inline fun <reified T : AppEntity> getRowNumByWorkaround(
  getAllEntitiesBlock: () -> List<T>,
  crossinline allEntitiesComparator: (T, T) -> Int,
  entity: T
): Int? {
  Log.e(TAG, "Failed to run ROW_NUMBER statement; falling back to getting all and sort")

  getAllEntitiesBlock()
    .sortedWith { a, b -> allEntitiesComparator(a, b) }
    .forEachIndexed { index: Int, current: T -> if (current.id == entity.id) return index }

  Log.w(TAG, "Could not find entity with primary key ${entity.id}")
  return null
}
