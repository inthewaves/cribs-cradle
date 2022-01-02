package org.welbodipartnership.cradle5.patients.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BaseDetailsCard(
  title: String?,
  modifier: Modifier = Modifier,
  backgroundColor: Color = MaterialTheme.colors.surface,
  columnContent: @Composable () -> Unit,
) {
  Card(
    elevation = 4.dp,
    backgroundColor = backgroundColor,
    shape = MaterialTheme.shapes.small,
    modifier = modifier
  ) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
      title?.let {
        Text(title, style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(4.dp))
      }
      columnContent()
    }
  }
}
