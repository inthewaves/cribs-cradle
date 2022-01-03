package org.welbodipartnership.cradle5.domain.auth

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AuthState {
  @Immutable
  object Initializing : AuthState
  @Immutable
  object LoggedOut : AuthState
  @Immutable
  class TokenExpired(val username: String) : AuthState
  @Immutable
  class LoggedInUnlocked(val username: String) : AuthState
  @Immutable
  class LoggedInLocked(val username: String) : AuthState
}
