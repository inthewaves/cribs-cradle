package org.welbodipartnership.cradle5.domain.auth

import java.lang.Exception

class FacilityParallelDownloadException(val result: AuthRepository.LoginResult) : Exception()
