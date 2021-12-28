package org.welbodipartnership.libmsn.api.patients

import org.welbodipartnership.libmsn.api.Verifiable

data class Patient(
  val initials: String,
): Verifiable {
  override fun verify() {
    TODO("Not yet implemented")
  }
}
