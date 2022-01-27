package org.welbodipartnership.cradle5.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.welbodipartnership.cradle5.BuildConfig
import org.welbodipartnership.cradle5.domain.UrlProvider
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {
  @Provides
  @Named(UrlProvider.DEFAULT_API_URL)
  fun provideBaseApiUrl(): String = BuildConfig.DEFAULT_API_URL

  @Provides
  @Named(UrlProvider.PRODUCTION_API_URL)
  fun provideProductionApiUrl(): String = BuildConfig.PRODUCTION_API_URL

  @Provides
  @Named(UrlProvider.TEST_API_URL)
  fun provideTestApiUrl(): String = BuildConfig.TEST_API_URL

  @Provides
  @Singleton
  fun providesOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30L, TimeUnit.SECONDS)
    .readTimeout(30L, TimeUnit.SECONDS)
    .writeTimeout(30L, TimeUnit.SECONDS)
    .build()
}
