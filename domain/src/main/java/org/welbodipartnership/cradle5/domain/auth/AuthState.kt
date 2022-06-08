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
  data class ForcedRelogin(val username: String) : AuthState
  @Immutable
  data class TokenExpired(val username: String) : AuthState
  @Immutable
  data class LoggedInUnlocked(val username: String) : AuthState
  @Immutable
  data class LoggedInLocked(val username: String) : AuthState
  @Immutable
  data class BlockingWarningMessage(val warningMessage: String) : AuthState
}
