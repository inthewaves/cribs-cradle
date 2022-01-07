package org.welbodipartnership.cradle5.domain

import android.content.Context
import android.util.Log
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.BufferedSource
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val JSON_MEDIA_TYPE = "application/json".toMediaType()

@Singleton
@PublishedApi
internal class HttpClient @Inject constructor(
  val okHttpClient: OkHttpClient,
  val appCoroutineDispatchers: AppCoroutineDispatchers,
) {

  /**
   * Enumeration of common HTTP method request types.
   */
  enum class Method { GET, POST, PUT, DELETE }

  /**
   * Performs an HTTP request with the connection's [BufferedSource] read by [successReader],
   * and then returns the result of [successReader] if the connection and download were
   * successful.
   *
   * For cases where putting an entire response in memory isn't optional (e.g., a large JSON array
   * is given by the server), specify [Unit] as the type parameter T, and then have the
   * [successReader] handle elements one by one from the [BufferedSource].
   *
   * @param method the request method
   * @param url where to send the request
   * @param headers HTTP headers to include with the request. Content-Type and Content-Encoding
   * headers are not needed.
   * @param requestBody An optional body to send with the request. If doing a POST or PUT, this
   * should not be null. Use [buildJsonRequestBody] to create a ResponseBody with the correct
   * JSON Content-Type headers applied. This is null by default.
   * @param successReader A function that processes the [BufferedSource]. If [makeRequest]
   * returns a [NetworkResult.Success], the value inside of the [NetworkResult.Success] will be the
   * return value of
   * the [successReader]. The [successReader] is only called if the server returns a
   * successful response code. Not expected to close the given [BufferedSource]. Note:
   * [IOException]s will be caught by [makeRequest] and return a [NetworkResult.NetworkException] as
   * the result.
   * @return The result of the network request: [NetworkResult.Success] if it succeeds,
   * [NetworkResult.Failure] if the server
   * returns a non-successful status code, and [NetworkResult.NetworkException] if an [IOException]
   * occurs.
   *
   * @throws IllegalArgumentException - if url is not a valid HTTP or HTTPS URL, or if using an
   * HTTP method that requires a non-null [requestBody] (like POST or PUT).
   */
  suspend inline fun <SuccessT, FailT> makeRequest(
    method: Method,
    url: String,
    headers: Map<String, String>,
    requestBody: RequestBody? = null,
    crossinline failureReader: suspend (BufferedSource, Headers) -> FailT,
    crossinline successReader: suspend (BufferedSource, Headers) -> SuccessT,
  ): NetworkResult<SuccessT, FailT> = makeRequestInternal(
    method,
    { url(url) },
    headers,
    requestBody,
    failureReader,
    successReader
  )

  suspend inline fun <SuccessT, FailT> makeRequest(
    method: Method,
    url: HttpUrl,
    headers: Map<String, String>,
    requestBody: RequestBody? = null,
    crossinline failureReader: suspend (BufferedSource, Headers) -> FailT,
    crossinline successReader: suspend (BufferedSource, Headers) -> SuccessT,
  ): NetworkResult<SuccessT, FailT> = makeRequestInternal(
    method,
    { url(url) },
    headers,
    requestBody,
    failureReader,
    successReader
  )

  @PublishedApi
  internal suspend inline fun <SuccessT, FailT> makeRequestInternal(
    method: Method,
    crossinline urlSetup: Request.Builder.() -> Unit,
    headers: Map<String, String>,
    requestBody: RequestBody? = null,
    crossinline failureReader: suspend (BufferedSource, Headers) -> FailT,
    crossinline successReader: suspend (BufferedSource, Headers) -> SuccessT,
  ): NetworkResult<SuccessT, FailT> = withContext(appCoroutineDispatchers.io) {
    val request = Request.Builder().apply {
      urlSetup()
      // Note: OkHttp will transparently handle accepting and decoding gzip responses.
      // No need to put a Content-Encoding header here.
      headers.forEach { (name, value) -> addHeader(name, value) }
      // This will throw an unchecked exception if requestBody is null when trying to do
      // HTTP methods that need a body.
      method(method.name, requestBody)
    }.build()

    val message = "${method.name} ${request.url}"
    try {
      runInterruptible { okHttpClient.newCall(request).execute() }.use {
        if (it.isSuccessful) {
          Log.i(TAG, "$message - Success ${it.code}")
          // The byte stream is closed by the `use` function above.
          NetworkResult.Success(successReader(it.body!!.source(), it.headers), it.code)
        } else {
          Log.i(TAG, "$message - Failure ${it.code}")
          NetworkResult.Failure(failureReader(it.body!!.source(), it.headers), it.code)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "$message - Exception", e)
      ensureActive()
      NetworkResult.NetworkException(e)
    }
  }

  companion object {
    const val TAG = "HttpClient"

    /**
     * Builds a [RequestBody] using an OutputStream to write. Since the exact size of the response
     * isn't known, OkHttp will use chunked streaming (via ChunkedSource).
     *
     * Use this if the body to send is too big to store in memory at once.
     */
    @Suppress("unused")
    inline fun buildJsonRequestBody(crossinline outputWriter: (BufferedSink) -> Unit) =
      object : RequestBody() {
        override fun contentType() = JSON_MEDIA_TYPE

        override fun writeTo(sink: BufferedSink) {
          outputWriter(sink)
        }
      }

    /**
     * Builds a [RequestBody] using the [byteArray]. Can be optimized for fixed streaming since the
     * exact length is available.
     *
     * Use this if the request body is small enough (not some big array or object) to not run into
     * OutOfMemoryErrors.
     */
    fun buildJsonRequestBody(byteArray: ByteArray) =
      object : RequestBody() {
        override fun contentType() = JSON_MEDIA_TYPE

        override fun contentLength() = byteArray.size.toLong()

        override fun writeTo(sink: BufferedSink) {
          sink.write(byteArray)
        }
      }
  }
}

typealias DefaultNetworkResult<SuccessT> = NetworkResult<SuccessT, ByteArray>

sealed interface NetworkResult<SuccessT, FailT> {
  fun valueOrNull(): SuccessT? = if (this is Success) value else null

  /**
   * Casts this error variant to a different type
   */
  fun <OtherSuccess> castError(): NetworkResult<OtherSuccess, FailT> = when (this) {
    is Failure -> {
      // must succeed; failure does not use the original SuccessT type anywhere
      @Suppress("UNCHECKED_CAST")
      this as Failure<OtherSuccess, FailT>
    }
    is NetworkException -> {
      // must succeed; NetworkException does not use the original SuccessT type anywhere
      @Suppress("UNCHECKED_CAST")
      this as NetworkException<OtherSuccess, FailT>
    }
    is Success -> error("cannot use castError to cast a success")
  }

  /**
   * The result of a successful network request.
   *
   * A request is considered successful if the response has a status code in the 200..=300 range.
   *
   * @property value The result value
   * @property statusCode Status code of the response which generated this result
   */
  data class Success<SuccessT, FailT>(
    val value: SuccessT,
    val statusCode: Int
  ) : NetworkResult<SuccessT, FailT>

  /**
   * The result of a network request which made it to the server but the status
   * code of the response indicated a failure (e.g., 404, 500, etc.).
   *
   * Contains the response status code along with the response body as a byte
   * array. Note that the body is not of type [T] like in [Success] since the
   * response for a failed request may not be the same type as the response for
   * a successful request.
   *
   * @property errorValue The body of the response
   * @property statusCode The status code of the response
   */
  data class Failure<SuccessT, FailT>(
    val errorValue: FailT,
    val statusCode: Int
  ) : NetworkResult<SuccessT, FailT>

  /**
   * Represents exception thrown during a request
   *
   * @property cause the exception which caused the failure
   */
  @JvmInline
  value class NetworkException<SuccessT, FailT>(
    val cause: Exception
  ) : NetworkResult<SuccessT, FailT> {
    fun formatErrorMessage(context: Context) =
      "(${cause::class.java.simpleName}) ${cause.localizedMessage}"
  }
}

/**
 * Maps a [NetworkResult.Success] value to another type using the given [transform]. For error
 * variants, this does nothing
 */
inline fun <SuccessT, FailT, OtherSuccess> NetworkResult<SuccessT, FailT>.mapSuccess(
  transform: (SuccessT) -> OtherSuccess
): NetworkResult<OtherSuccess, FailT> = if (this is NetworkResult.Success) {
  NetworkResult.Success(transform(this.value), this.statusCode)
} else {
  // must succeed; failure types does not use the original SuccessT type anywhere
  @Suppress("UNCHECKED_CAST")
  this as NetworkResult<OtherSuccess, FailT>
}

/**
 * Gets an error message. Since the failure result is just a byte array, we just read that if it
 * is a failure from the server.
 */
fun <T> DefaultNetworkResult<T>.getErrorMessageOrNull(context: Context): String? {
  return when (this) {
    is NetworkResult.Failure -> {
      "HTTP $statusCode error, error message ${this.errorValue.decodeToString()}"
    }
    is NetworkResult.NetworkException -> {
      formatErrorMessage(context)
    }
    is NetworkResult.Success -> null
  }
}
