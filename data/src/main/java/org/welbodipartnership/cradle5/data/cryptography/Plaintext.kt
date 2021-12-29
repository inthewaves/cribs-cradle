package org.welbodipartnership.cradle5.data.cryptography

@JvmInline
value class Plaintext(val bytes: ByteArray) {
  val size get() = bytes.size
}
