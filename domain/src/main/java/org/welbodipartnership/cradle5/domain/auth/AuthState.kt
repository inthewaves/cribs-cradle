package org.welbodipartnership.cradle5.domain.auth

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AuthState {
  val hasValidToken: Boolean

  @Immutable
  object Initializing : AuthState {
    override val hasValidToken = false
  }
  @Immutable
  object LoggingIn : AuthState {
    override val hasValidToken = false
  }
  @Immutable
  object LoggedOut : AuthState {
    override val hasValidToken = false
  }
  @Immutable
  class ForcedRelogin(override val hasValidToken: Boolean, val username: String) : AuthState
  @Immutable
  class LoggedInUnlocked(val username: String, override val hasValidToken: Boolean) : AuthState
  @Immutable
  class LoggedInLocked(val username: String, override val hasValidToken: Boolean) : AuthState
  @Immutable
  class BlockingWarningMessage(override val hasValidToken: Boolean, val warningMessage: String) : AuthState
}
