package org.welbodipartnership.cradle5.util.executors

import java.util.concurrent.Executor

data class AppExecutors constructor(
  val locationExecutor: Executor
)