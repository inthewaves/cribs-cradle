package org.welbodipartnership.cradle5.domain

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.welbodipartnership.api.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("UNUSED")
internal object MoshiModule {
  @Provides
  @Singleton
  fun providesMoshi(): Moshi = Json.buildMoshiInstanceForApi()
}
