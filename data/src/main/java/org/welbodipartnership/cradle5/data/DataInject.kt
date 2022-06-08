package org.welbodipartnership.cradle5.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.signal.argon2.Argon2
import org.signal.argon2.MemoryCost
import org.signal.argon2.Type
import org.signal.argon2.Version
import org.welbodipartnership.cradle5.data.appinit.DataAndEncryptionSetupTask
import org.welbodipartnership.cradle5.data.appinit.EnumDefaultVersionCheckJob
import org.welbodipartnership.cradle5.data.cryptography.ArgonHasher
import org.welbodipartnership.cradle5.util.appinit.AppInitTask
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("UNUSED")
object Argon2Module {
  @Provides
  @Singleton
  fun providesPasswordHasher(appCoroutineDispatchers: AppCoroutineDispatchers): ArgonHasher {
    // from https://github.com/signalapp/Signal-Android/blob/44d014c4459e9ac34b74800002fa86b402d0501c/app/src/main/java/org/thoughtcrime/securesms/lock/PinHashing.java#L21
    // which has been tested on some user devices
    // (https://github.com/signalapp/Signal-Android/commit/ace18557974a5400e5c4e3c6356802029ec09963
    return ArgonHasher(
      appCoroutineDispatchers = appCoroutineDispatchers,
      argon2 = Argon2.Builder(Version.V13)
        .type(Type.Argon2id)
        .iterations(32)
        .memoryCost(MemoryCost.MiB(16))
        .parallelism(1)
        .hashLength(64)
        .build(),
      argon2Username = Argon2.Builder(Version.V13)
        .type(Type.Argon2id)
        .iterations(16)
        .memoryCost(MemoryCost.MiB(4))
        .parallelism(1)
        .hashLength(16)
        .build(),
      // The spec (https://www.cryptolux.org/images/0/0d/Argon2.pdf) recommends 128 bits as a good
      // salt length
      saltLength = 16
    )
  }
}

@Module
@InstallIn(SingletonComponent::class)
@Suppress("UNUSED")
abstract class DataTaskModule {
  @Binds
  @IntoSet
  abstract fun bindDataSetupTask(bind: DataAndEncryptionSetupTask): AppInitTask

  @Binds
  @IntoSet
  abstract fun bindEnumDefaultVersionCheckTask(bind: EnumDefaultVersionCheckJob): AppInitTask
}
