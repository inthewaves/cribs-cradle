package org.welbodipartnership.cradle5.domain

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runInterruptible
import okhttp3.FormBody
import org.welbodipartnership.api.ApiAuthToken
import org.welbodipartnership.api.DynamicLookupBody
import org.welbodipartnership.api.IndexMenuItem
import org.welbodipartnership.api.LoginErrorMessage
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.data.settings.AuthToken
import org.welbodipartnership.cradle5.data.settings.authToken
import java.io.IOException
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestApi @Inject internal constructor(
  @PublishedApi
  internal val urlProvider: UrlProvider,
  @PublishedApi
  internal val moshi: Moshi,
  @PublishedApi
  internal val httpClient: HttpClient,
  private val appValuesStore: AppValuesStore
) {

  suspend fun login(username: String, password: String): NetworkResult<AuthToken, LoginErrorMessage?> {
    val requestBody = FormBody.Builder(Charsets.UTF_8)
      .addEncoded("username", username)
      .addEncoded("password", password)
      .addEncoded("grant_type", "password")
      .build()

    return httpClient.makeRequest(
      method = HttpClient.Method.POST,
      url = urlProvider.token,
      headers = emptyMap(),
      requestBody = requestBody,
      failureReader = { src ->
        runInterruptible {
          moshi.adapter(LoginErrorMessage::class.java).fromJson(src)
        }
      },
      successReader = { src ->
        val adapter: JsonAdapter<ApiAuthToken> = moshi.adapter(ApiAuthToken::class.java)
        val apiAuthToken: ApiAuthToken = runInterruptible { adapter.fromJson(src) }
          ?: throw IOException("missing token body")

        authToken {
          accessToken = apiAuthToken.accessToken
          tokenType = apiAuthToken.tokenType
          this.username = apiAuthToken.username
          issued = apiAuthToken.issued
          expires = apiAuthToken.expires
        }
      }
    )
  }

  @PublishedApi
  internal val defaultHeadersFlow: Flow<Map<String, String>> = appValuesStore.authTokenFlow
    .map { token ->
      token
        ?.let { Collections.singletonMap("Authorization", "Bearer ${it.accessToken}") }
        ?: emptyMap()
    }

  suspend fun getIndexEntries(): DefaultNetworkResult<List<IndexMenuItem>> {
    return httpClient.makeRequest(
      method = HttpClient.Method.GET,
      url = urlProvider.indexEndpoint,
      headers = defaultHeadersFlow.first(),
      failureReader = { src ->
        runInterruptible { src.readByteArray() }
      },
      successReader = { src ->
        runInterruptible {
          moshi.adapter<List<IndexMenuItem>>(
            Types.newParameterizedType(List::class.java, IndexMenuItem::class.java)
          ).fromJson(src) ?: throw IOException("missing index")
        }
      }
    )
  }

  suspend inline fun <reified T> getFormData(
    formId: FormId = FormId.fromAnnotationOrThrow<T>(),
    objectId: ObjectId
  ): DefaultNetworkResult<T> {
    return httpClient.makeRequest(
      method = HttpClient.Method.GET,
      url = urlProvider.formsDataOnly(formId, objectId),
      headers = defaultHeadersFlow.first(),
      failureReader = { src ->
        runInterruptible { src.readByteArray() }
      },
      successReader = { src ->
        runInterruptible {
          moshi.adapter<T>(T::class.java).fromJson(src)
            ?: throw IOException("missing item ${T::class.java.simpleName}")
        }
      }
    )
  }

  suspend inline fun <reified T> getDynamicLookupData(
    controlId: ControlId,
    formId: FormId,
    objectId: ObjectId,
    page: Int = 1
  ): DefaultNetworkResult<DynamicLookupBody<T>> {
    return httpClient.makeRequest(
      method = HttpClient.Method.GET,
      url = urlProvider.dynamicLookups(controlId, formId, objectId, page),
      headers = defaultHeadersFlow.first(),
      failureReader = { src ->
        runInterruptible { src.readByteArray() }
      },
      successReader = { src ->
        runInterruptible {
          moshi.adapter<DynamicLookupBody<T>>(
            Types.newParameterizedType(DynamicLookupBody::class.java, T::class.java)
          ).fromJson(src)
            ?: throw IOException("missing item ${T::class.java.simpleName}")
        }
      }
    )
  }

  companion object {
    private const val TAG = "RestApi"
  }
}
