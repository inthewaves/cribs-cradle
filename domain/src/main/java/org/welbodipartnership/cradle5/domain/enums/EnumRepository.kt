package org.welbodipartnership.cradle5.domain.enums

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.SendChannel
import org.welbodipartnership.api.lookups.LookupResult
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.data.settings.DynamicServerEnum
import org.welbodipartnership.cradle5.data.settings.dynamicServerEnum
import org.welbodipartnership.cradle5.domain.DefaultNetworkResult
import org.welbodipartnership.cradle5.domain.LookupId
import org.welbodipartnership.cradle5.domain.NetworkResult
import org.welbodipartnership.cradle5.domain.RestApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnumRepository @Inject constructor(
  private val restApi: RestApi,
  private val appValuesStore: AppValuesStore,
  @ApplicationContext private val context: Context,
) {

  sealed class DownloadResult {
    object Success : DownloadResult()
    data class Invalid(
      val errorMessage: String?,
      val errorCode: Int
    ) : DownloadResult()
    data class Exception(
      val cause: java.lang.Exception,
      val errorMessage: String?
    ) : DownloadResult()
  }

  private fun DefaultNetworkResult<*>.castErrorToDownloadResult(): DownloadResult {
    return when (this) {
      is NetworkResult.Success -> error("cannot cast success to failure")
      is NetworkResult.Failure -> DownloadResult.Invalid(
        this.errorValue.decodeToString(),
        this.statusCode
      )
      is NetworkResult.NetworkException -> DownloadResult.Exception(
        this.cause,
        formatErrorMessage(context)
      )
    }
  }

  suspend fun downloadAndSaveEnumsFromServer(eventChannel: SendChannel<String>?): DownloadResult {
    val lookupsList = when (val result = restApi.getAllPossibleLookups()) {
      is NetworkResult.Success -> result.value
      else -> return result.castErrorToDownloadResult()
    }

    Log.d(TAG, "received ${lookupsList.size} lookups / enums from the server")

    val enumsList: List<DynamicServerEnum> = buildList {
      for (entry in lookupsList) {
        eventChannel?.trySend("Downloading dropdown values for ${entry.name}")
        val lookupId = LookupId(entry.id)
        when (val lookupGetResult = restApi.getLookupValues(lookupId)) {
          is NetworkResult.Success -> {
            add(lookupGetResult.value.toProtoDynamicServerEnum())
          }
          else -> return lookupGetResult.castErrorToDownloadResult()
        }
      }
    }

    try {
      appValuesStore.replaceEnums(enumsList)
    } catch (e: IOException) {
      return DownloadResult.Exception(e, e.localizedMessage)
    }

    return DownloadResult.Success
  }

  private fun LookupResult.toProtoDynamicServerEnum(): DynamicServerEnum {
    val apiResponse = this

    return dynamicServerEnum {
      name = apiResponse.name
      id = apiResponse.id
      values.addAll(
        apiResponse.values.asSequence()
          .map { apiLookupValue ->
            DynamicServerEnum.Value.newBuilder()
              .setName(apiLookupValue.name)
              .setCode(apiLookupValue.code)
              .setId(apiLookupValue.id)
              .setListOrder(apiLookupValue.listOrder)
              .build()
          }
          .asIterable()
      )
    }
  }

  companion object {
    private const val TAG = "EnumManager"
  }
}
