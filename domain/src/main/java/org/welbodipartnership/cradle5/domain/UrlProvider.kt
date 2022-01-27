package org.welbodipartnership.cradle5.domain

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.welbodipartnership.cradle5.data.settings.AppValuesStore
import org.welbodipartnership.cradle5.data.settings.ServerType
import org.welbodipartnership.cradle5.util.ApplicationCoroutineScope
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@JvmInline
value class FormId(val id: Int) {
  companion object {
    inline fun <reified T> fromAnnotationOrThrow(): FormId {
      val annotation = requireNotNull(
        T::class.java.getAnnotation(org.welbodipartnership.api.forms.FormId::class.java)
      ) { "${T::class.java} does not have a FormId annotation!" }
      return FormId(annotation.id)
    }
  }
}

@JvmInline
value class ObjectId(val id: Int) {
  companion object {
    val QUERIES = ObjectId(0)
    val NEW_POST = ObjectId(0)
  }
}

@JvmInline
value class NodeId(val id: Int)

@JvmInline
value class LookupId(val id: Int)

@JvmInline
value class ControlId(val id: String)

interface IUrlProvider {
  val userFriendlySiteUrl: String
  val userFriendlySiteUrlFlow: StateFlow<String>
}

@Singleton
class UrlProvider @Inject constructor(
  @Named(DEFAULT_API_URL) val defaultApiUrl: String,
  @Named(PRODUCTION_API_URL) val productionApiUrl: String,
  @Named(TEST_API_URL) val testApiUrl: String,
  @ApplicationCoroutineScope appCoroutineScope: CoroutineScope,
  appValuesStore: AppValuesStore,
) : IUrlProvider {
  companion object {
    private const val TAG = "UrlProvider"

    const val DEFAULT_API_URL = "defaultApiUrl"
    const val PRODUCTION_API_URL = "productionApiUrl"
    const val TEST_API_URL = "testApiUrl"
  }

  private val _baseApiUrl = appValuesStore.serverUrlOverrideFlow
    .map { serverUrlType: ServerType? ->
      serverUrlType?.let {
        Log.d(TAG, "detected url override: $it")
        when (it) {
          ServerType.PRODUCTION -> productionApiUrl
          ServerType.TEST -> testApiUrl
          ServerType.UNRECOGNIZED, ServerType.UNSET -> defaultApiUrl
        }
      } ?: defaultApiUrl
    }
    .stateIn(
      appCoroutineScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000L),
      initialValue = defaultApiUrl
    )
  private val baseApiUrl: String get() = _baseApiUrl.value

  override val userFriendlySiteUrl: String get() = baseApiUrl.removeSuffix("/api")

  override val userFriendlySiteUrlFlow: StateFlow<String> = _baseApiUrl
    .map { it.removeSuffix("/api") }
    .stateIn(
      appCoroutineScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000L),
      initialValue = _baseApiUrl.value.removeSuffix("/api")
    )

  val token get() = "$baseApiUrl/v0/token"

  val indexEndpoint get() = "$baseApiUrl/"

  fun forms(
    formId: FormId,
    objectId: ObjectId,
    basePatientId: ObjectId? = null,
  ) = "$baseApiUrl/v0/forms/${formId.id}/${objectId.id}".let {
    if (basePatientId != null) {
      it.toHttpUrl().newBuilder()
        .addQueryParameter("baseId", basePatientId.id.toString())
        .build()
        .toString()
    } else {
      it
    }
  }

  fun forms(nodeId: NodeId) =
    "$baseApiUrl/v0/forms/${nodeId.id}"

  fun formsDataOnly(formId: FormId, objectId: ObjectId) =
    "$baseApiUrl/v0/forms/dataonly/${formId.id}/${objectId.id}"

  fun formsDataOnly(nodeId: NodeId) =
    "$baseApiUrl/v0/forms/dataonly/${nodeId.id}"

  fun lists(formId: FormId) =
    "$baseApiUrl/v0/lists/${formId.id}"

  fun listsDataOnly(nodeId: NodeId) =
    "$baseApiUrl/v0/lists/dataonly/${nodeId.id}"

  /**
   * Gets the list of lookups (i.e. server enums) used in the database
   */
  fun lookupsList() =
    "$baseApiUrl/v0/lookups"

  /**
   * Gets the list of lookups (i.e. server enums) used in the database
   */
  fun lookups(id: LookupId) =
    "$baseApiUrl/v0/lookups/${id.id}"

  /**
   * Returns the values of a dynamic lookup for forms that may not be in the patient’s
   */
  fun dynamicLookups(controlId: ControlId, formId: FormId, objectId: ObjectId, page: Int) =
    "$baseApiUrl/v0/lookups/dynamic/${controlId.id}/${formId.id}/${objectId.id}".let {
      if (page > 1) {
        it.toHttpUrl().newBuilder()
          .addQueryParameter("page", page.toString())
          .build()
          .toString()
      } else {
        it
      }
    }

  fun dynamicLookups(controlId: ControlId, nodeId: NodeId) =
    "$baseApiUrl/v0/lookups/dynamic/${controlId.id}/${nodeId.id}"

  fun tree(basePatientId: ObjectId) =
    "$baseApiUrl/v0/tree/${basePatientId.id}"
}
