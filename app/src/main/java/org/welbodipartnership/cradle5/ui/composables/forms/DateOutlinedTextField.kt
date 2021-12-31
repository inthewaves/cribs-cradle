package org.welbodipartnership.cradle5.ui.composables.forms

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.suspendCancellableCoroutine
import org.welbodipartnership.cradle5.R
import org.welbodipartnership.cradle5.ui.composables.OutlinedTextFieldWithErrorHint
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.date.FormDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

@Composable
fun DateOutlinedTextField(
  date: FormDate?,
  onDatePicked: (FormDate) -> Unit,
  onPickerClose: () -> Unit = {},
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  textStyle: TextStyle = LocalTextStyle.current,
  label: @Composable (() -> Unit)? = null,
  placeholder: @Composable (() -> Unit)? = {
    Text(stringResource(R.string.date_text_field_tap_to_set_date_placeholder))
  },
  leadingIcon: @Composable (() -> Unit)? = { Icon(Icons.Filled.EditCalendar, null) },
  trailingIcon: @Composable (() -> Unit)? = null,
  errorHint: String? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = false,
  maxLines: Int = Int.MAX_VALUE,
  shape: Shape = MaterialTheme.shapes.small,
  colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val channelState = remember { mutableStateOf<SendChannel<Unit>?>(null) }
  DisposableEffect(date, scope) {
    val newChannel = scope.actor<Unit>(capacity = Channel.RENDEZVOUS) {
      for (unused in channel) {
        val activity = requireNotNull(context.getFragmentActivity()) { "failed to get activity" }
        val nowDate: Long = with(LocalDate.now()) {
          val dateTime = LocalDateTime.of(year, month, dayOfMonth, 0, 0)
          dateTime.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC))
            .toInstant()
            .toEpochMilli()
        }
        val existingDate = date?.toGmtGregorianCalendar()?.timeInMillis
        val datePicker = MaterialDatePicker.Builder.datePicker()
          .setSelection(existingDate)
          .setCalendarConstraints(
            CalendarConstraints.Builder().apply {
              setEnd(nowDate)
              if (existingDate != null) {
                setOpenAt(existingDate)
              }
            }.build()
          )
          .build()

        try {
          val finalDate: FormDate? = suspendCancellableCoroutine { cont ->
            datePicker.addOnPositiveButtonClickListener {
              Log.d("MainActivity", "addOnPosListener")
              val calendar =
                GregorianCalendar(TimeZone.getTimeZone("GMT")).apply { time = Date(it) }
              cont.resume(
                FormDate(
                  day = calendar[Calendar.DAY_OF_MONTH],
                  month = calendar[Calendar.MONTH] + 1,
                  year = calendar[Calendar.YEAR]
                ),
                null
              )
            }
            datePicker.addOnCancelListener {
              cont.resume(null, null)
            }
            datePicker.addOnNegativeButtonClickListener {
              cont.resume(null, null)
            }
            datePicker.show(activity.supportFragmentManager, "date-picker")
          }
          if (finalDate != null) {
            onDatePicked(finalDate)
          }
          onPickerClose()
        } finally {
          datePicker.clearOnPositiveButtonClickListeners()
          datePicker.clearOnCancelListeners()
          datePicker.clearOnNegativeButtonClickListeners()
        }
      }
    }
    channelState.value = newChannel
    onDispose {
      newChannel.close()
      channelState.value = null
    }
  }

  OutlinedTextFieldWithErrorHint(
    value = date?.toString() ?: "",
    onValueChange = {},
    modifier = Modifier,
    textFieldModifier = textFieldModifier,
    enabled = enabled,
    readOnly = true,
    textStyle = textStyle,
    label = label,
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    errorHint = errorHint,
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    maxLines = maxLines,
    interactionSource = remember { MutableInteractionSource() }
      .also { interactionSource ->
        LaunchedEffect(interactionSource, channelState) {
          interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
              channelState.value?.trySend(Unit)
            }
          }
        }
      },
    shape = shape,
    colors = colors
  )
}

@Preview
@Composable
fun DateOutlinedTextFieldPreview() {
  CradleTrialAppTheme {
    Scaffold {
      Column {
        var selectedDate: MutableState<FormDate?> = remember { mutableStateOf(null) }


        Text("")
        DateOutlinedTextField(
          date = selectedDate.value,
          onDatePicked = {
            selectedDate.value = it
          }
        )
      }
    }
  }
}

private tailrec fun Context.getFragmentActivity(): FragmentActivity? {
  return when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.getFragmentActivity()
    else -> null
  }
}
