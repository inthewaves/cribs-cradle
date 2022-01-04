package org.welbodipartnership.cradle5.ui.composables.screenlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScreenListItem(
  minHeight: Dp,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
  rowContent: @Composable RowScope.() -> Unit
) {
  Column(modifier) {
    val outlineModifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
    Row(
      modifier = Modifier.then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
      )
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .fillMaxWidth()
        .heightIn(min = minHeight),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = horizontalArrangement
    ) {
      rowContent()
    }
    Box(outlineModifier)
  }
}
