package org.welbodipartnership.cradle5.compose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle

class SavedStateMutableState<T>(
  private val handle: SavedStateHandle,
  private val key: String,
  defaultValue: T,
) : MutableState<T> {

  private val mutableState: MutableState<T>

  init {
    val savedValue = handle.get<T>(key)
    mutableState = mutableStateOf(savedValue ?: defaultValue)
  }

  override var value: T
    get() = mutableState.value
    set(value) {
      set(value)
    }

  private fun set(new: T) {
    mutableState.value = new
    handle[key] = new
  }

  override fun component1(): T = value

  override fun component2(): (T) -> Unit = ::set
}

fun <T> SavedStateHandle.createMutableState(key: String, defaultValue: T) =
  SavedStateMutableState(this, key, defaultValue)
