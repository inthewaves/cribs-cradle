package org.welbodipartnership.cradle5.domain

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runInterruptible
import okhttp3.FormBody
import okhttp3.Headers
import okio.BufferedSource
import org.welbodipartnership.api.ApiAuthToken
import org.welbodipartnership.api.IndexMenuItem
import org.welbodipartnership.api.LoginErrorMessage
import org.welbodipartnership.api.forms.FormGetResponse
import org.welbodipartnership.api.forms.FormPostBody
import org.welbodipartnership.api.lookups.LookupResult
import org.welbodipartnership.api.lookups.LookupsEnumerationEntry
import org.welbodipartnership.api.lookups.dynamic.DynamicLookupBody
import org.welbodipartnership.cradle5.data.database.entities.Outcomes
import org.welbodipartnership.cradle5.data.database.entities.Patient
import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo
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

  val nodeIdFromHeaderReader: suspend (BufferedSource, Headers) -> NodeId = { _, headers ->
    val locationHeader = headers["Location"]
      ?: throw IOException("missing expected location header")
    val nodeIdString = locationHeader.substringAfterLast('/', "")
      .ifEmpty { null }
      ?: throw IOException("location header $locationHeader doesn't have a path")
    val nodeId = nodeIdString.toIntOrNull()
      ?: throw IOException("location header $locationHeader doesn't end in an integer")
    NodeId(nodeId)
  }

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
      failureReader = { src, _ ->
        runInterruptible {
          moshi.adapter(LoginErrorMessage::class.java).fromJson(src)
        }
      },
      successReader = { src, _ ->
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
      failureReader = { src, _ ->
        runInterruptible { src.readByteArray() }
      },
      successReader = { src, _ ->
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
      failureReader = { src, _ ->
        runInterruptible { src.readByteArray() }
      },
      successReader = { src, _ ->
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
      failureReader = { src, _ ->
        runInterruptible { src.readByteArray() }
      },
      successReader = { src, _ ->
        runInterruptible {
          moshi.adapter<DynamicLookupBody<T>>(
            Types.newParameterizedType(DynamicLookupBody::class.java, T::class.java)
          ).fromJson(src)
            ?: throw IOException("missing item ${T::class.java.simpleName}")
        }
      }
    )
  }

  suspend fun getAllPossibleLookups(): DefaultNetworkResult<List<LookupsEnumerationEntry>> {
    return httpClient.makeRequest(
      method = HttpClient.Method.GET,
      url = urlProvider.lookupsList(),
      headers = defaultHeadersFlow.first(),
      failureReader = { src, _ ->
        runInterruptible { src.readByteArray() }
      },
      successReader = { src, _ ->
        runInterruptible {
          moshi.adapter<List<LookupsEnumerationEntry>>(
            Types.newParameterizedType(List::class.java, LookupsEnumerationEntry::class.java)
          ).fromJson(src)
            ?: throw IOException("missing lookups list")
        }
      }
    )
  }

  suspend fun getLookupValues(lookupId: LookupId): DefaultNetworkResult<LookupResult> {
    return httpClient.makeRequest(
      method = HttpClient.Method.GET,
      url = urlProvider.lookups(lookupId),
      headers = defaultHeadersFlow.first(),
      failureReader = { src, _ ->
        runInterruptible { src.readByteArray() }
      },
      successReader = { src, _ ->
        runInterruptible {
          moshi.adapter(LookupResult::class.java).fromJson(src)
            ?: throw IOException("missing lookups result")
        }
      }
    )
  }

  sealed class PostResult {
    data class AllFailed(
      val failOrException: NetworkResult<*, ByteArray>?,
      val otherCause: Exception? = null
    ) : PostResult()

    data class ObjectIdRetrievalFailed(
      val failOrException: NetworkResult<*, ByteArray>?,
      val otherCause: Exception? = null,
      val partialServerInfo: ServerInfo,
    ) : PostResult()

    class Success(val serverInfo: ServerInfo) : PostResult()
  }

  /**
   * Posts a patient to the database and returns the nodeId of the patient. The nodeId is given
   * in the Location header.
   */
  private suspend inline fun <reified PostType> postNewFormSubmission(
    submission: PostType,
    urlProvider: (FormId) -> String,
  ): PostResult {
    val postBody: FormPostBody<PostType>
    val adapter: JsonAdapter<FormPostBody<PostType>> = try {
      postBody = FormPostBody.create(submission)
      moshi.adapter(Types.newParameterizedType(FormPostBody::class.java, PostType::class.java))
    } catch (e: Exception) {
      Log.e(
        TAG,
        "postNewFormSubmission: ${PostType::class.java.simpleName} does not have an adapter",
        e
      )
      return PostResult.AllFailed(failOrException = null, otherCause = e)
    }
    val formId = try {
      FormId.fromAnnotationOrThrow<PostType>()
    } catch (e: Exception) {
      Log.e(
        TAG,
        "postNewFormSubmission: ${PostType::class.java.simpleName} does FormId annotation",
        e
      )
      return PostResult.AllFailed(failOrException = null, otherCause = e)
    }

    val body = HttpClient.buildJsonRequestBody { sink -> adapter.toJson(sink, postBody) }

    val nodeId: NodeId = when (
      val result = httpClient.makeRequest(
        method = HttpClient.Method.POST,
        url = urlProvider(formId),
        headers = defaultHeadersFlow.first(),
        requestBody = body,
        failureReader = { src, _ -> src.readByteArray() },
        successReader = nodeIdFromHeaderReader
      )
    ) {
      is NetworkResult.Success -> result.value
      else -> {
        Log.e(TAG, "postNewFormSubmission: failed to post ${PostType::class.java.simpleName}")
        when (result) {
          is NetworkResult.Failure -> {
            Log.e(TAG, "postNewFormSubmission: failure: ${result.statusCode}, ${result.errorValue.decodeToString()}")
          }
          is NetworkResult.NetworkException -> TODO()
          is NetworkResult.Success -> {}
        }
        return PostResult.AllFailed(result)
      }
    }
    Log.d(TAG, "postNewFormSubmission: posted and got NodeId ${nodeId.id}. Obtaining objectId")

    val objectId = when (val result = getObjectId(nodeId)) {
      is NetworkResult.Success -> result.value
      else -> {
        Log.e(TAG, "failed to get ObjectId")
        return PostResult.ObjectIdRetrievalFailed(
          result,
          otherCause = null,
          ServerInfo(nodeId = nodeId.id.toLong(), objectId = null)
        )
      }
    }

    return PostResult.Success(
      ServerInfo(nodeId = nodeId.id.toLong(), objectId = objectId.id.toLong())
    )
  }

  /**
   * Gets the objectId for a form with the given [nodeId].
   */
  private suspend fun getObjectId(nodeId: NodeId): DefaultNetworkResult<ObjectId> {
    return httpClient.makeRequest(
      method = HttpClient.Method.GET,
      url = urlProvider.forms(nodeId),
      headers = defaultHeadersFlow.first(),
      failureReader = { src, _ -> src.readByteArray() },
      successReader = { src, _ ->
        // this adapter doesn't consume the entire body
        val objectId = FormGetResponse.ObjectIdOnlyAdapter.fromJson(src)
          ?: throw IOException("missing ObjectId for nodeId $nodeId")
        ObjectId(objectId)
      }
    )
  }

  suspend fun postPatient(patient: Patient): PostResult {
    return postNewFormSubmission(
      patient.toApiBody(),
      urlProvider = { formId -> urlProvider.forms(formId, ObjectId.NEW_POST) }
    )
  }

  suspend fun postOutcomes(outcomes: Outcomes, patientObjectId: ObjectId): PostResult {
    return postNewFormSubmission(
      outcomes.toApiBody(),
      urlProvider = { formId -> urlProvider.forms(formId, ObjectId.NEW_POST, patientObjectId) }
    )
  }

  companion object {
    @PublishedApi
    internal const val TAG = "RestApi"
  }
}
