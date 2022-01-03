package org.welbodipartnership.cradle5.domain.auth

sealed interface AuthState {
  object Initializing : AuthState
  object LoggedOut : AuthState
  class TokenExpired(val username: String) : AuthState
  class LoggedInUnlocked(val username: String) : AuthState
  class LoggedInLocked(val username: String) : AuthState
}
