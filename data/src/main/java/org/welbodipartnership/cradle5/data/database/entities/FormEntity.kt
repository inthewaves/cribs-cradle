package org.welbodipartnership.cradle5.data.database.entities

import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo

sealed interface FormEntity : AppEntity {
  val serverInfo: ServerInfo?

  val isUploadedToServer: Boolean get() = serverInfo?.nodeId != null || serverInfo?.objectId != null
}
