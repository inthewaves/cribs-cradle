package org.welbodipartnership.cradle5.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.welbodipartnership.cradle5.util.CoroutineModule

@InstallIn(SingletonComponent::class)
@Module(
  includes = [
    CoroutineModule::class
  ]
)
object AppModule
