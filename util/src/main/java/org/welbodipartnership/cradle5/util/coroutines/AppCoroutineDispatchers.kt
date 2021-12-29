package org.welbodipartnership.cradle5.util.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher

data class AppCoroutineDispatchers(
  val io: CoroutineDispatcher,
  val main: MainCoroutineDispatcher,
  val default: CoroutineDispatcher,
  val unconfined: CoroutineDispatcher,
)
