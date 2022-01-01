package org.welbodipartnership.cradle5.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.welbodipartnership.cradle5.data.appinit.DataAndEncryptionSetupTask
import org.welbodipartnership.cradle5.util.appinit.AppInitTask

@Module
@InstallIn(SingletonComponent::class)
@Suppress("UNUSED")
abstract class DataTaskModule {
  @Binds
  @IntoSet
  abstract fun bindDataSetupTask(bind: DataAndEncryptionSetupTask): AppInitTask
}
