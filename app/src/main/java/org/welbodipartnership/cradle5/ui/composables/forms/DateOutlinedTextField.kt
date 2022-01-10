package org.welbodipartnership.cradle5.ui.composables.forms

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.TextFieldColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
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
import org.welbodipartnership.cradle5.ui.theme.CradleTrialAppTheme
import org.welbodipartnership.cradle5.util.datetime.FormDate
import org.welbodipartnership.cradle5.util.datetime.toFormDateFromNoSlashesOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

private const val TAG = "DateOutlinedTextField"

val formDateToTimestampMapper: (dateString: String) -> Long? = { dateString ->
  dateString.toFormDateFromNoSlashesOrNull()
    ?.toGmtGregorianCalendar()
    ?.timeInMillis
}

val timestampToFormDateMapper: (timestampMillis: Long) -> String = {
  FormDate.fromGmtTimestampMillis(it).toString(withSlashes = false)
}

/**
 * https://stackoverflow.com/a/68471076
 *
 * 00/00/0000
 */
class DateTransformation : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
    val out = buildString(trimmed.length) {
      for (i in trimmed.indices) {
        append(trimmed[i])
        if (i % 2 == 1 && i < 4) {
          append('/')
        }
      }
    }

    val numberOffsetTranslator = object : OffsetMapping {
      override fun originalToTransformed(offset: Int): Int {
        if (offset <= 1) return offset
        if (offset <= 3) return offset + 1
        if (offset <= 8) return offset + 2
        return 10
      }

      override fun transformedToOriginal(offset: Int): Int {
        if (offset <= 2) return offset
        if (offset <= 5) return offset - 1
        if (offset <= 10) return offset - 2
        return 8
      }
    }

    return TransformedText(AnnotatedString(out), numberOffsetTranslator)
  }
}

/**
 * [dateStringToTimestampMapper] is used by the date picker to map [text] to a possible picker
 * value to use, and [timestampToDateStringMapper] is used to convert picker-selected timestamps
 * into strings into [onValueChange].
 */
@Composable
fun DateOutlinedTextField(
  text: String,
  onValueChange: (String) -> Unit,
  dateStringToTimestampMapper: (dateString: String) -> Long?,
  timestampToDateStringMapper: (timestampMillis: Long) -> String,
  modifier: Modifier = Modifier,
  helpButtonText: String? = null,
  maxLength: Int = Int.MAX_VALUE,
  onPickerClose: () -> Unit = {},
  textFieldModifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  textStyle: TextStyle = LocalTextStyle.current,
  label: @Composable (() -> Unit)? = null,
  placeholder: @Composable (() -> Unit)? = null,
  errorHint: String? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = false,
  maxLines: Int = 1,
  shape: Shape = MaterialTheme.shapes.small,
  colors: TextFieldColors = darkerDisabledOutlinedTextFieldColors()
) {
  val channelState = remember { mutableStateOf<SendChannel<Unit>?>(null) }
  if (enabled) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    DisposableEffect(text, scope, dateStringToTimestampMapper) {
      val newChannel = scope.actor<Unit>(capacity = Channel.RENDEZVOUS) {
        for (unused in channel) {
          val activity = requireNotNull(context.getFragmentActivity()) { "failed to get activity" }
          val nowDate: Long = with(LocalDate.now()) {
            val dateTime = LocalDateTime.of(year, month, dayOfMonth, 0, 0)
            dateTime.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC))
              .toInstant()
              .toEpochMilli()
          }
          val existingDate = dateStringToTimestampMapper(text)
          val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(existingDate)
            .setCalendarConstraints(
              try {
                CalendarConstraints.Builder().apply {
                  setEnd(nowDate)
                  if (existingDate != null) {
                    setOpenAt(existingDate)
                  }
                }.build()
              } catch (e: IllegalArgumentException) {
                Log.w(TAG, "failed to create constraints", e)
                CalendarConstraints.Builder().setEnd(nowDate).build()
              }
            )
            .build()

          try {
            val pickedTimestampMillis: Long? = suspendCancellableCoroutine { cont ->
              datePicker.addOnPositiveButtonClickListener {
                cont.resume(it, null)
              }
              datePicker.addOnCancelListener {
                cont.resume(null, null)
              }
              datePicker.addOnNegativeButtonClickListener {
                cont.resume(null, null)
              }
              datePicker.show(activity.supportFragmentManager, "date-picker")
            }
            if (pickedTimestampMillis != null) {
              val newDateString = timestampToDateStringMapper(pickedTimestampMillis)
              onValueChange(newDateString)
            }
            onPickerClose()
          } finally {
            datePicker.apply {
              clearOnPositiveButtonClickListeners()
              clearOnCancelListeners()
              clearOnNegativeButtonClickListeners()
            }
          }
        }
      }
      channelState.value = newChannel
      onDispose {
        newChannel.close()
        channelState.value = null
      }
    }
  }

  OutlinedTextFieldWithErrorHint(
    value = text,
    onValueChange = {
      if (it.length <= maxLength) onValueChange(it)
    },
    modifier = modifier,
    textFieldModifier = textFieldModifier,
    enabled = enabled,
    readOnly = readOnly,
    textStyle = textStyle,
    label = label,
    placeholder = placeholder,
    trailingIcon = {
      Row {
        IconButton(
          onClick = { channelState.value?.trySend(Unit) },
          enabled = enabled && !readOnly,
        ) {
          Icon(
            imageVector = Icons.Filled.EditCalendar,
            contentDescription = stringResource(R.string.date_text_field_icon_button_cd),
          )
        }
        if (helpButtonText != null) {
          MoreInfoIconButton(moreInfoText = helpButtonText,)
        }
      }
    },
    errorHint = errorHint,
    visualTransformation = DateTransformation(),
    keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number),
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    maxLines = maxLines,
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
        val selectedDate: MutableState<String> = remember { mutableStateOf("") }

        DateOutlinedTextField(
          text = selectedDate.value,
          onValueChange = { selectedDate.value = it },
          dateStringToTimestampMapper = formDateToTimestampMapper,
          timestampToDateStringMapper = timestampToFormDateMapper
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
