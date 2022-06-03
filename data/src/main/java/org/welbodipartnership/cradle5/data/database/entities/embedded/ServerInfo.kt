package org.welbodipartnership.cradle5.data.database.entities.embedded

import java.time.ZonedDateTime

/**
 * Contains server info. Every entity on the server represented by a form (a "registration",
 * "outcome"), and every form represents a node on the server.
 */
data class ServerInfo(
  /**
   * Unique ID for this object. This can be used to access the object directly on the server via the
   * forms/{nodeId} API endpoint.
   *
   * We make this nullable, because while there are tree-based forms where we always expect the
   * server to return the nodeId as the Location header to a POST request, there are also non-tree
   * forms that don't have node IDs
   */
  val nodeId: Long?,
  /**
   * Data row's ID in its respective table.
   *
   * We make this nullable, since there can be edge cases where we can't get the objectId yet.
   */
  val objectId: Long?,
  val updateTime: ZonedDateTime?,
  val createTime: ZonedDateTime?,
)
