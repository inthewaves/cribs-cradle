package org.welbodipartnership.cradle5.data.database.entities

import org.welbodipartnership.cradle5.data.database.entities.embedded.ServerInfo

interface FormEntity : AppEntity {
  val serverInfo: ServerInfo?
}
