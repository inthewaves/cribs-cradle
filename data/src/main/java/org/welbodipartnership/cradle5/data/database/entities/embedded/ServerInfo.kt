package org.welbodipartnership.cradle5.data.database.entities.embedded

/**
 * Contains server info. Every entity on the server represented by a form (a "registration",
 * "outcome"), and every form represents a node on the server.
 */
data class ServerInfo(
  /**
   * Unique ID for this object. This can be used to access the object directly on the server via the
   * forms/{nodeId} API endpoint.
   */
  val nodeId: Long,
  /** Data row's ID in its respective table. */
  val objectId: Long,
)
