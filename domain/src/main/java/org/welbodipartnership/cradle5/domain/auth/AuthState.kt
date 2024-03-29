package org.welbodipartnership.cradle5.domain.auth

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AuthState {
  @Immutable
  object Initializing : AuthState
  @Immutable
  object LoggingIn : AuthState
  @Immutable
  object LoggedOut : AuthState
  @Immutable
  class ForcedRelogin(val username: String) : AuthState
  @Immutable
  class LoggedInUnlocked(val username: String, val isTokenExpired: Boolean) : AuthState
  @Immutable
  class LoggedInLocked(val username: String) : AuthState
  @Immutable
  class BlockingWarningMessage(val warningMessage: String) : AuthState
}
