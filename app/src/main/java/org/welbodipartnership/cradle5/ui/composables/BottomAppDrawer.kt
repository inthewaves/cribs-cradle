package org.welbodipartnership.cradle5.ui.composables

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.BottomDrawer
import androidx.compose.material.BottomDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BottomAppDrawer(
  drawerState: BottomDrawerState,
  modifier: Modifier = Modifier,
  drawerContent: @Composable ColumnScope.() -> Unit,
  content: @Composable () -> Unit
) {
  BottomDrawer(
    drawerState = drawerState,
    modifier = modifier,
    drawerContent = drawerContent,
    content = content,
  )
}
