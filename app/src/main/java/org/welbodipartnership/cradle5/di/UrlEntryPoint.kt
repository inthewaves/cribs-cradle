package org.welbodipartnership.cradle5.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.welbodipartnership.cradle5.domain.UrlProvider

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UrlEntryPoint {
  fun getUrlProvider(): UrlProvider
}
