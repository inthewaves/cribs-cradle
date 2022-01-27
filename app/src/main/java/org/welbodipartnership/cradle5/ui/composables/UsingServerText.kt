package org.welbodipartnership.cradle5.ui.composables

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.welbodipartnership.cradle5.domain.IUrlProvider
import org.welbodipartnership.cradle5.util.launchWebIntent

@Composable
fun UsingServerText(
  urlProvider: IUrlProvider,
  modifier: Modifier = Modifier
) {
  val serverUrl by urlProvider.userFriendlySiteUrlFlow.collectAsState()
  val isOverrideActive by urlProvider.isOverrideActive.collectAsState()
  val annotated = buildAnnotatedString {
    append("Using ")
    pushStringAnnotation("serverUrl", serverUrl)
    withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
      append(urlProvider.userFriendlySiteUrl)
    }
    if (isOverrideActive) {
      append(" (using override from login screen)")
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

val LocalUrlProvider: ProvidableCompositionLocal<IUrlProvider> = compositionLocalOf {
  // the actual UrlProvider should be provided inside of MainActivity
  object : IUrlProvider {
    private val backingFlow = MutableStateFlow("https://exampleThisServerDoesntWork.com")
    override val userFriendlySiteUrl: String = backingFlow.value
    override val userFriendlySiteUrlFlow: StateFlow<String> = backingFlow
    override val isOverrideActive: StateFlow<Boolean> = MutableStateFlow(false)
  }
}
