package org.welbodipartnership.cradle5.domain

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.welbodipartnership.api.forms.meta.ControlType
import org.welbodipartnership.api.forms.meta.DataType
import org.welbodipartnership.cradle5.util.datetime.FormDate
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("UNUSED")
internal object MoshiModule {
  @Provides
  @Singleton
  fun providesMoshi(): Moshi = Moshi.Builder()
    .add(FormDate::class.java, FormDate.Adapter())
    .add(DataType::class.java, EnumJsonAdapter.create(DataType::class.java))
    .add(ControlType::class.java, EnumJsonAdapter.create(ControlType::class.java))
    .build()
}
