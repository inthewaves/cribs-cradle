package org.welbodipartnership.cradle5.util

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import javax.inject.Singleton

@Module
object CoroutineModule {
  @Singleton
  @Provides
  fun provideCoroutineDispatchers() = AppCoroutineDispatchers(
    io = Dispatchers.IO,
    main = Dispatchers.Main,
    default = Dispatchers.Default,
    unconfined = Dispatchers.Unconfined
  )
}
