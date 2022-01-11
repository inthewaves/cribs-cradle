package org.welbodipartnership.cradle5.domain

import android.content.Context
import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runInterruptible
import okhttp3.FormBody
import okhttp3.Headers
import okio.BufferedSource
import okio.buffer
import okio.source
import org.welbodipartnership.api.ApiAuthToken
import org.welbodipartnership.api.IndexMenuItem
import org.welbodipartnership.api.LoginErrorMessage
import org.welbodipartnership.api.cradle5.GpsForm
import org.welbodipartnership.api.forms.FormGetResponse
import org.welbodipartnership.api.forms.FormPostBody
import org.welbodipartnership.api.forms.PostFailureBody
import org.welbodipartnership.api.lookups.LookupResult
import org.welbodipartnership.api.lookups.LookupsEnumerationEntry
import org.welbodipartnership.api.lookups.dynamic.DynamicLookupBody
import org.welbodipartnership.cradle5.data.database.entities.FormEntity
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
  private val appValuesStore: AppValuesStore,
  @ApplicationContext private val context: Context
) {

  // ---- General unauthed calls

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

  // ---- Authorized calls

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

  suspend fun <T> getDynamicLookupData(
    valuesClass: Class<T>,
    controlId: ControlId,
    formId: FormId,
    objectId: ObjectId,
  ): DefaultNetworkResult<List<T>> {

    val firstPageRes: DefaultNetworkResult<DynamicLookupBody<T>> = getPageForDynamicLookup(
      valuesClass,
      controlId,
      formId,
      objectId,
      page = 1,
    )

    if (firstPageRes !is NetworkResult.Success) {
      return firstPageRes.castError()
    }
    if (firstPageRes.value.pageNumber >= firstPageRes.value.totalNumberOfPages) {
      // fast path: avoid allocating an new list and just returned the (mapped) result
      return firstPageRes.mapSuccess { it.results }
    }
    Log.d(
      TAG,
      "Dynamic lookup $controlId, $formId, $objectId " +
        "has more pages: total pages = ${firstPageRes.value.totalNumberOfPages}; " +
        "total number of records: ${firstPageRes.value.totalNumberOfRecords}"
    )
    // copy and pasted code, but we're optimizing by not creating this list builder if there's
    // only one page
    return firstPageRes.mapSuccess {
      buildList<T>(capacity = firstPageRes.value.totalNumberOfRecords) {
        addAll(firstPageRes.value.results)

        val totalPages = firstPageRes.value.totalNumberOfPages
        var iterations = 0
        var currentPageResult: NetworkResult.Success<DynamicLookupBody<T>, ByteArray> =
          firstPageRes
        do {
          val pageNow = currentPageResult.value.pageNumber + 1
          val thisPageResult: DefaultNetworkResult<DynamicLookupBody<T>> =
            getPageForDynamicLookup(
              valuesClass,
              controlId,
              formId,
              objectId,
              page = pageNow,
            )
          if (thisPageResult !is NetworkResult.Success) {
            return thisPageResult.castError()
          }

          addAll(thisPageResult.value.results)
          currentPageResult = thisPageResult
          iterations++
        } while (
          currentPageResult.value.pageNumber < totalPages &&
          iterations < MAX_LIST_PAGINATION_RECURSION
        )

        if (iterations >= MAX_LIST_PAGINATION_RECURSION) {
          Log.w(
            TAG,
            "Dynamic lookup $controlId, $formId, $objectId: server returned too many pages; " +
              "iterated $iterations times"
          )
        }
      }
    }
  }

  private suspend fun <T> getPageForDynamicLookup(
    valuesClass: Class<T>,
    controlId: ControlId,
    formId: FormId,
    objectId: ObjectId,
    page: Int
  ): DefaultNetworkResult<DynamicLookupBody<T>> {
    require(page >= 1) { "page should be a positive integer" }
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
            Types.newParameterizedType(DynamicLookupBody::class.java, valuesClass)
          ).fromJson(src)
            ?: throw IOException("missing item for dynamic lookup")
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

  // ---- Post requests for new form entries

  /**
   * A reader to read a newly posted form entry's [NodeId] from the header. When a new form entry
   * is made via a POST request, the API sends back the NodeId as a link in the Location header.
   */
  private val nodeIdFromHeaderReader: suspend (BufferedSource, Headers) -> NodeId = { _, headers ->
    val locationHeader = headers["Location"]
      ?: throw IOException("missing expected location header")
    val nodeIdString = locationHeader.substringAfterLast('/', "")
      .ifEmpty { null }
      ?: throw IOException("location header $locationHeader doesn't have a path")
    val nodeId = nodeIdString.toIntOrNull()
      ?: throw IOException("location header $locationHeader doesn't end in an integer")
    NodeId(nodeId)
  }

  /**
   * Encapsulates the result of a sequence of POST and GET requests for uploading data to the API.
   *
   * When we send a POST request to the server, the API does not give us the objectId. It only
   * gives us the nodeId in the form of a URL in the response Location header. We have to follow
   * the nodeId in order to get the objectId. This
   */
  sealed interface PostResult {
    /**
     * If the entity happens to have been uploaded fully (not expected to happen),
     * the
     */
    data class AlreadyUploaded(val serverInfo: ServerInfo) : PostResult {
      init {
        requireNotNull(serverInfo.nodeId) { "missing nodeId for AlreadyUploaded" }
        requireNotNull(serverInfo.objectId) { "missing objectId for AlreadyUploaded" }
      }
    }

    /**
     * Represents a POST request where the initial posting of the data failed somehow. This can
     * come from either network errors or the server returning a non-successful HTTP code for the
     * POST request.
     *
     * An error can also come if the Location header is missing or changes format (e.g. no longer
     * reflects the node id). However, this is rather unexpected. Regardless, it might be prudent
     * to try to handle this case e.g. if it returns a URL, just blindly follow the URL and try to
     * read the data from there. This approach comes with other issues, however; the GET request
     * on a form's data does not contain the nodeId in the Meta object.
     */
    data class AllFailed(
      val failOrException: NetworkResult<*, ByteArray>?,
      val failureBody: PostFailureBody?,
      val otherCause: Exception? = null
    ) : PostResult

    /**
     * R
     */
    data class ObjectIdRetrievalFailed(
      val failOrException: NetworkResult<*, ByteArray>?,
      val otherCause: Exception? = null,
      val partialServerInfo: ServerInfo,
    ) : PostResult

    /**
     * Guaranteed to have objectId and nodeId non-null
     */
    @JvmInline
    value class Success(val serverInfo: ServerInfo) : PostResult {
      init {
        requireNotNull(serverInfo.nodeId) { "missing nodeId for Success" }
        requireNotNull(serverInfo.objectId) { "missing objectId for Success" }
      }
    }
  }

  /**
   * Performs a POST and GET request sequence as documented in [PostResult].
   */
  private suspend inline fun <
    reified DbEntity : FormEntity,
    reified PostType
    > multiStageNewFormSubmission(
    entityToUpload: DbEntity,
    transform: (DbEntity) -> PostType,
    urlProvider: (FormId) -> String,
  ): PostResult {
    val serverInfo = entityToUpload.serverInfo

    val nodeId: NodeId = if (serverInfo?.nodeId != null) {
      if (serverInfo.objectId != null) {
        Log.w(
          TAG,
          "multiStageNewFormSubmission: " +
            "trying to post ${PostType::class.java.simpleName} that has already been uploaded"
        )
        return PostResult.AlreadyUploaded(serverInfo)
      }
      Log.d(
        TAG,
        "multiStageNewFormSubmission" +
          "entity ${PostType::class.java.simpleName} with id ${entityToUpload.id} has been " +
          "partially uploaded"
      )
      NodeId(serverInfo.nodeId.toInt())
    } else {
      val submission: PostType = try {
        transform(entityToUpload)
      } catch (e: Exception) {
        Log.e(TAG, "transform failed", e)
        return PostResult.AllFailed(null, null, e)
      }
      val postBody: FormPostBody<PostType>
      val adapter: JsonAdapter<FormPostBody<PostType>> = try {
        postBody = FormPostBody.create(submission)
        moshi.adapter(Types.newParameterizedType(FormPostBody::class.java, PostType::class.java))
      } catch (e: Exception) {
        Log.e(
          TAG,
          "multiStageNewFormSubmission: " +
            "${PostType::class.java.simpleName} does not have an adapter",
          e
        )
        return PostResult.AllFailed(failOrException = null, failureBody = null, otherCause = e)
      }
      val formId = try {
        FormId.fromAnnotationOrThrow<PostType>()
      } catch (e: Exception) {
        Log.e(
          TAG,
          "multiStageNewFormSubmission: " +
            "${PostType::class.java.simpleName} missing FormId annotation",
          e
        )
        return PostResult.AllFailed(failOrException = null, failureBody = null, otherCause = e)
      }

      val body = HttpClient.buildJsonRequestBody { sink -> adapter.toJson(sink, postBody) }

      when (
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
        is NetworkResult.Failure -> {
          Log.e(
            TAG,
            "multiStageNewFormSubmission: " +
              "failed to POST ${PostType::class.java.simpleName}: " +
              result.getErrorMessageOrNull(context)
          )

          val failureBody: PostFailureBody? = try {
            if (result.statusCode == 400) {
              val failureAdapter = moshi.adapter(PostFailureBody::class.java)
              failureAdapter.fromJson(result.errorValue.inputStream().source().buffer())
            } else {
              null
            }
          } catch (e: Exception) {
            Log.e(TAG, "failed to parse error message")
            null
          }

          return PostResult.AllFailed(result, failureBody, null)
        }
        is NetworkResult.NetworkException -> {
          Log.e(
            TAG,
            "multiStageNewFormSubmission: " +
              "exception during POST ${PostType::class.java.simpleName}: " +
              result.getErrorMessageOrNull(context)
          )
          return PostResult.AllFailed(result, null, null)
        }
      }.also {
        Log.d(
          TAG,
          "multiStageNewFormSubmission: " +
            "POSTed ${PostType::class.java.simpleName} & got NodeId ${it.id}. " +
            "Now getting ObjectId"
        )
      }
    }

    val objectId = when (val result = getObjectId(nodeId)) {
      is NetworkResult.Success -> result.value
      else -> {
        Log.e(
          TAG,
          "multiStageNewFormSubmission: failed to get ObjectId for NodeId ${nodeId.id}: " +
            result.getErrorMessageOrNull(context)
        )
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

  /**
   * Post a patient to the server. This will perform a POST request to send the data and grab the
   * nodeId of the patient, and then it will send a GET request to the server's Meta info in order
   * to get the patient's objectId.
   *
   * If the [patient] has non-null server info, then it will fail if it has both a nodeId and
   * objectId. Otherwise, we interpret it as a partial upload
   *
   * Note that posting a patient and posting a patient's outcomes are done in separate calls.
   * Posting outcomes has to be done by calling [multiStagePostOutcomes].
   */
  suspend fun multiStagePostPatient(patient: Patient): PostResult {
    return multiStageNewFormSubmission(
      patient,
      { it.toApiBody() },
      urlProvider = { formId -> urlProvider.forms(formId, ObjectId.NEW_POST) }
    )
  }

  /**
   * Post an outcome for the patient with the given [patientObjectId] to the server. Performs a
   * POST-and-GET-request sequence.
   *
   * The [patientObjectId] is the patient's object ID obtained from the server. It can only be done
   * from a successful call to [multiStagePostPatient]
   */
  suspend fun multiStagePostOutcomes(outcomes: Outcomes, patientObjectId: ObjectId): PostResult {
    return multiStageNewFormSubmission(
      outcomes,
      { it.toApiBody() },
      urlProvider = {
        formId ->
        urlProvider.forms(
          formId = formId,
          objectId = ObjectId.NEW_POST,
          basePatientId = patientObjectId
        )
      }
    )
  }

  suspend fun postGpsForm(gpsForm: GpsForm): DefaultNetworkResult<Unit> {
    val postBody: FormPostBody<GpsForm>
    val adapter: JsonAdapter<FormPostBody<GpsForm>> = try {
      postBody = FormPostBody.create(gpsForm)
      moshi.adapter(Types.newParameterizedType(FormPostBody::class.java, GpsForm::class.java))
    } catch (e: Exception) {
      Log.e(TAG, "postGpsForm: missing adapter or annotatiion", e)
      return NetworkResult.NetworkException(e)
    }
    val formId = FormId.fromAnnotationOrThrow<GpsForm>()
    val body = HttpClient.buildJsonRequestBody { sink -> adapter.toJson(sink, postBody) }
    return httpClient.makeRequest(
      method = HttpClient.Method.POST,
      url = urlProvider.forms(formId, ObjectId.NEW_POST),
      headers = defaultHeadersFlow.first(),
      requestBody = body,
      failureReader = { src, _ -> src.readByteArray() },
      successReader = { _, _ -> Unit }
    )
  }

  companion object {
    @PublishedApi
    internal const val TAG = "RestApi"

    private const val MAX_LIST_PAGINATION_RECURSION = 100
  }
}
