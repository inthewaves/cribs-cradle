package org.welbodipartnership.cradle5.domain.auth

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AuthState {
  object Initializing : AuthState
  object LoggedOut : AuthState
  class TokenExpired(val username: String) : AuthState
  @Immutable
  class LoggedInUnlocked(val username: String) : AuthState
  class LoggedInLocked(val username: String) : AuthState
}
