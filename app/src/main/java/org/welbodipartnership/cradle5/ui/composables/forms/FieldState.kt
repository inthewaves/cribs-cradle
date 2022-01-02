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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext

open class TextFieldState(
  validator: (String) -> Boolean,
  errorFor: (Context, String) -> String,
  initialValue: String = "",
  backingState: MutableState<String> = mutableStateOf(initialValue),
) : FieldState<String>(validator, errorFor, initialValue, backingState) {
  override val showErrorOnInput: Boolean = false
}

/**
 * Derived from official Compose samples (Jetsurvey). There's likely a better way to handle this.
 */
abstract class FieldState<T>(
  val validator: (T) -> Boolean = { true },
  val errorFor: (Context, T) -> String = { _, _ -> "" },
  val initialValue: T,
  val backingState: MutableState<T> = mutableStateOf(initialValue),
) {
  abstract val showErrorOnInput: Boolean

  var stateValue: T
    get() = backingState.value
    set(value) {
      if (showErrorOnInput) {
        isFocusedDirty = true
      }

      backingState.value = value
      onNewStateValue(value)
    }

  open fun onNewStateValue(newValue: T) {}

  // was the TextField ever focused
  var isFocusedDirty by mutableStateOf(false)
  var isFocused: Boolean by mutableStateOf(false)
  private var displayErrors: Boolean by mutableStateOf(false)

  fun reset() {
    isFocusedDirty = showErrorOnInput
    isFocused = false
    displayErrors = false
    stateValue = initialValue
  }

  val isValid: Boolean by derivedStateOf { validator(stateValue) }

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FieldState<*>

    if (showErrorOnInput != other.showErrorOnInput) return false
    if (stateValue != other.stateValue) return false
    if (isFocusedDirty != other.isFocusedDirty) return false
    if (isFocused != other.isFocused) return false

    return true
  }

  override fun hashCode(): Int {
    var result = stateValue?.hashCode() ?: 0
    result = 31 * result + isFocusedDirty.hashCode()
    result = 31 * result + isFocused.hashCode()
    return result
  }


}
