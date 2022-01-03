package org.welbodipartnership.cradle5.util

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.welbodipartnership.cradle5.util.appinit.AppInitTask
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import org.welbodipartnership.cradle5.util.foreground.AppForegroundedObserverSetupTask
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * A CoroutineScope for the entire application process. No need to cancel this, because
 * it dies when the app process dies.
 *
 * See https://medium.com/androiddevelopers/create-an-application-coroutinescope-using-hilt-dd444e721528
 * or https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
 * for more details
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
@MustBeDocumented
annotation class ApplicationCoroutineScope

@Module
@InstallIn(SingletonComponent::class)
@Suppress("UNUSED")
object CoroutineModule {
  @Singleton
  @Provides
  fun provideCoroutineDispatchers() = AppCoroutineDispatchers(
    io = Dispatchers.IO,
    main = Dispatchers.Main,
    default = Dispatchers.Default,
    unconfined = Dispatchers.Unconfined
  )

  @Singleton
  @ApplicationCoroutineScope
  @Provides
  fun providesApplicationCoroutineScope(
    appCoroutineDispatchers: AppCoroutineDispatchers,
  ): CoroutineScope = CoroutineScope(SupervisorJob() + appCoroutineDispatchers.default)
}

@Module
@InstallIn(SingletonComponent::class)
@Suppress("UNUSED")
abstract class AppInitTaskModule {
  @Binds
  @IntoSet
  abstract fun bindForegroundObserverTask(bind: AppForegroundedObserverSetupTask): AppInitTask
}
