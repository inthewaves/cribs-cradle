package org.welbodipartnership.cradle5.ui.composables

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dagger.hilt.EntryPoints
import org.welbodipartnership.cradle5.BuildConfig
import org.welbodipartnership.cradle5.di.UrlEntryPoint
import org.welbodipartnership.cradle5.domain.UrlProvider
import org.welbodipartnership.cradle5.util.launchWebIntent

@Composable
fun UsingServerText(
  urlProvider: UrlProvider,
  modifier: Modifier = Modifier
) {
  val annotated = buildAnnotatedString {
    append("Using ")
    pushStringAnnotation("serverUrl", urlProvider.userFriendlySiteUrl)
    withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
      append(urlProvider.userFriendlySiteUrl)
    }
  }

  val context = LocalContext.current
  ClickableMaterialText(
    annotated,
    modifier = modifier
  ) { offset ->
    annotated.getStringAnnotations("serverUrl", start = offset, end = offset)
      .firstOrNull()
      ?.let { context.launchWebIntent(urlProvider.userFriendlySiteUrl) }
  }
}

val LocalUrlProvider: ProvidableCompositionLocal<UrlProvider> = compositionLocalOf {
  // the actual UrlProvider should be provided inside of MainActivity
  UrlProvider("https://exampleThisServerDoesntWork.com")
}
