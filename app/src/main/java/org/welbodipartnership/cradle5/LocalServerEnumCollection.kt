package org.welbodipartnership.cradle5

import androidx.compose.runtime.compositionLocalOf
import org.welbodipartnership.cradle5.data.serverenums.ServerEnumCollection

val LocalServerEnumCollection = compositionLocalOf { ServerEnumCollection.defaultInstance }
