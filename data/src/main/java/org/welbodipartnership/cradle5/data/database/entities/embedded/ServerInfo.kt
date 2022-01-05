package org.welbodipartnership.cradle5.data.database.entities.embedded

/**
 * Contains server info. Every entity on the server represented by a form (a "registration",
 * "outcome"), and every form represents a node on the server.
 */
data class ServerInfo(
  /**
   * Unique ID for this object. This can be used to access the object directly on the server via the
   * forms/{nodeId} API endpoint.
   *
   * We make this non-nullable, because we always expect the server to return the nodeId as the
   * Location header to a POST request.
   */
  val nodeId: Long,
  /**
   * Data row's ID in its respective table.
   *
   * We make this nullable, since there can be edge cases where we can't get the objectId yet.
   */
  val objectId: Long?,
)
