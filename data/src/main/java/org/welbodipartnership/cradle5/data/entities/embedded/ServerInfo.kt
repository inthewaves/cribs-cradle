package org.welbodipartnership.cradle5.data.entities.embedded

/**
 * Contains server info. Every entity on the server represented by a form (a "registration",
 * "outcome") is a node on the server.
 */
data class ServerInfo(
  /** Unique ID for this object */
  val nodeId: Long,
  /** Data row's ID in its respective table */
  val objectId: Long,
)
