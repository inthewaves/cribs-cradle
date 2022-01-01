package org.welbodipartnership.cradle5.data.settings

import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppKeyValueStore @Inject constructor(
  private val encryptedSettings: EncryptedSettings
) {
  fun getServerEnumCollection() = ServerEnumCollection.defaultInstance
}
