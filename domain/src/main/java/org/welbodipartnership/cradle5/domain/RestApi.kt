package org.welbodipartnership.cradle5.domain

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runInterruptible
import okhttp3.FormBody
import org.welbodipartnership.api.ApiAuthToken
import org.welbodipartnership.api.LoginErrorMessage
import org.welbodipartnership.cradle5.data.settings.AuthToken
import org.welbodipartnership.cradle5.data.settings.authToken
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestApi @Inject internal constructor(
  private val urlProvider: UrlProvider,
  private val moshi: Moshi,
  private val httpClient: HttpClient,
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

  companion object {
    private const val TAG = "RestApi"
  }
}
