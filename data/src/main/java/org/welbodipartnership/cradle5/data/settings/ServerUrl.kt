package org.welbodipartnership.cradle5.data.settings

@JvmInline
value class ServerUrl internal constructor(val url: String) {
  override fun toString(): String = url
}
