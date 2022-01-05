package org.welbodipartnership.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.EnumJsonAdapter
import org.welbodipartnership.api.forms.FormGetResponse
import org.welbodipartnership.api.forms.meta.ControlType
import org.welbodipartnership.api.forms.meta.DataType
import org.welbodipartnership.cradle5.util.datetime.FormDate

object Json {
  fun buildMoshiInstanceForApi(): Moshi = Moshi.Builder()
    .add(FormDate::class.java, FormDate.Adapter())
    .add(DataType::class.java, EnumJsonAdapter.create(DataType::class.java))
    .add(ControlType::class.java, EnumJsonAdapter.create(ControlType::class.java))
    .build()
}

inline fun <reified T> Moshi.getAdapterForFormType(): JsonAdapter<FormGetResponse<T>> {
  return adapter(Types.newParameterizedType(FormGetResponse::class.java, T::class.java))
}
