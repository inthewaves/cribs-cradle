package org.welbodipartnership.cradle5.util.coroutines

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

fun <T> LiveData<T>.asFlow(appCoroutineDispatchers: AppCoroutineDispatchers): Flow<T> =
  callbackFlow {
    val observer = Observer<T> { value -> trySend(value) }
    observeForever(observer)
    awaitClose {
      removeObserver(observer)
    }
  }.flowOn(appCoroutineDispatchers.main.immediate)
