package org.welbodipartnership.cradle5.data.database.entities.embedded

import java.time.ZonedDateTime
import java.util.Date

/**
 * Contains server info. Every entity on the server represented by a form (a "registration",
 * "outcome"), and every form represents a node on the server.
 */
data class ServerInfo(
  /**
   * Unique ID for this object. This can be used to access the object directly on the server via the
   * forms/{nodeId} API endpoint.
   *
   * We make this nullable, because not really dealing with forms with tree structure
   */
  val nodeId: Long?,
  /**
   * Data row's ID in its respective table.
   *
   * Non-nullable, because server returns the ID
   */
  val objectId: Long,
  /**
   * Nullable because this requires an extra stage during upload to resolve. However, this might be
   * null if it's the first time uploading.
   */
  val updateTime: ZonedDateTime?,
  /** Nullable because this requires an extra stage during upload to resolve */
  val createdTime: ZonedDateTime?
)
