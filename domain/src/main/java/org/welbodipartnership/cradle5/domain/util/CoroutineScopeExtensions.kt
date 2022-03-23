package org.welbodipartnership.cradle5.domain.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

inline fun CoroutineScope.launchWithPermit(
  semaphore: Semaphore,
  crossinline block: suspend CoroutineScope.() -> Unit
) = launch {
  semaphore.withPermit { block() }
}
