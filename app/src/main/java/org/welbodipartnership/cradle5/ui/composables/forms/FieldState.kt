/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.welbodipartnership.cradle5.ui.composables.forms

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext

open class TextFieldState(
  validator: (String) -> Boolean,
  errorFor: (Context, String) -> String,
  initialValue: String = ""
) : FieldState<String>(validator, errorFor, initialValue) {
  override val showErrorOnInput: Boolean = false
  override var stateValue: String by mutableStateOf(initialValue)
}

abstract class FieldState<T>(
  val validator: (T) -> Boolean = { true },
  private val errorFor: (Context, T) -> String = { _, _ -> "" },
  val initialValue: T,
) {
  abstract val showErrorOnInput: Boolean

  abstract var stateValue: T
  // was the TextField ever focused
  var isFocusedDirty: Boolean by mutableStateOf(showErrorOnInput)
  var isFocused: Boolean by mutableStateOf(false)
  private var displayErrors: Boolean by mutableStateOf(false)

  fun reset() {
    isFocusedDirty = showErrorOnInput
    isFocused = false
    displayErrors = false
    stateValue = initialValue
  }

  open val isValid: Boolean
    get() = validator(stateValue)

  fun createFocusChangeModifier() = Modifier
    .onFocusChanged { focusState ->
      onFocusChange(focusState.isFocused)
      if (!focusState.isFocused) {
        enableShowErrors()
      }
    }

  private fun onFocusChange(focused: Boolean) {
    isFocused = focused
    if (focused) isFocusedDirty = true
  }

  fun enableShowErrors(force: Boolean = false) {
    // only show errors if the text was at least once focused
    if (isFocusedDirty || force) {
      displayErrors = true
    }
  }

  fun showErrors() = !isValid && displayErrors

  @Composable
  open fun getError(): String? {
    return if (showErrors()) {
      errorFor(LocalContext.current, stateValue)
    } else {
      null
    }
  }
}
