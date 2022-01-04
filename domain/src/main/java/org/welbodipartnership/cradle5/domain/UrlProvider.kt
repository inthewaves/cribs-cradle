package org.welbodipartnership.cradle5.domain

import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@JvmInline
value class FormId(val id: Long)

@JvmInline
value class ObjectId(val id: Long) {
  companion object {
    val QUERIES = ObjectId(0)
  }
}

@JvmInline
value class NodeId(val id: Long)

@JvmInline
value class LookupId(val id: Long)

@JvmInline
value class ControlId(val id: String)

@Singleton
class UrlProvider @Inject constructor(@Named("baseApiUrl") val baseApiUrl: String) {
  val token =
    "$baseApiUrl/v0/token"

  val indexEndpoint = "$baseApiUrl/"

  fun forms(formId: FormId, objectId: ObjectId) =
    "$baseApiUrl/v0/forms/${formId.id}/${objectId.id}"

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
