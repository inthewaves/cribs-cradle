package org.welbodipartnership.cradle5.ui.composables.forms

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExposedDropdownMenuBoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMaxBy
import kotlinx.coroutines.coroutineScope
import kotlin.math.max

/**
 * Subscribes to onGlobalLayout and correctly removes the callback when the View is detached.
 * Logic copied from AndroidPopup.android.kt.
 */
private class OnGlobalLayoutListener(
  private val view: View,
  private val onGlobalLayoutCallback: () -> Unit
) : View.OnAttachStateChangeListener, ViewTreeObserver.OnGlobalLayoutListener {
  private var isListeningToGlobalLayout = false

  init {
    view.addOnAttachStateChangeListener(this)
    registerOnGlobalLayoutListener()
  }

  override fun onViewAttachedToWindow(p0: View) = registerOnGlobalLayoutListener()

  override fun onViewDetachedFromWindow(p0: View) = unregisterOnGlobalLayoutListener()

  override fun onGlobalLayout() = onGlobalLayoutCallback()

  private fun registerOnGlobalLayoutListener() {
    if (isListeningToGlobalLayout || !view.isAttachedToWindow) return
    view.viewTreeObserver.addOnGlobalLayoutListener(this)
    isListeningToGlobalLayout = true
  }

  private fun unregisterOnGlobalLayoutListener() {
    if (!isListeningToGlobalLayout) return
    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
    isListeningToGlobalLayout = false
  }

  fun dispose() {
    unregisterOnGlobalLayoutListener()
    view.removeOnAttachStateChangeListener(this)
  }
}

private fun updateHeight(
  view: View,
  coordinates: LayoutCoordinates?,
  verticalMarginInPx: Int,
  onHeightUpdate: (Int) -> Unit
) {
  coordinates ?: return
  val visibleWindowBounds = Rect().let {
    view.getWindowVisibleDisplayFrame(it)
    it
  }
  val heightAbove = coordinates.boundsInWindow().top - visibleWindowBounds.top
  val heightBelow =
    visibleWindowBounds.bottom - visibleWindowBounds.top - coordinates.boundsInWindow().bottom
  onHeightUpdate(max(heightAbove, heightBelow).toInt() - verticalMarginInPx)
}

private fun Modifier.expandable(
  onExpandedChange: () -> Unit,
) = pointerInput(Unit) {
  forEachGesture {
    coroutineScope {
      awaitPointerEventScope {

        var event: PointerEvent
        var firstEventTime: Long = 0
        do {
          event = awaitPointerEvent(PointerEventPass.Initial)
          if (firstEventTime == 0L) {
            firstEventTime = (event.changes.fastMaxBy { -it.uptimeMillis }?.uptimeMillis ?: 0L)
          }
        } while (
          !event.changes.fastAll { it.changedToUp() }
        )

        val anyChanges = event.changes.fastAny { it.positionChanged() }
        val newEventTime = (event.changes.fastMaxBy { -it.uptimeMillis }?.uptimeMillis ?: 0L)
        if (!anyChanges && newEventTime - firstEventTime < viewConfiguration.longPressTimeoutMillis) {
          onExpandedChange.invoke()
        }
      }
    }
  }
}.semantics {
  // contentDescription = menuLabel // this should be a localised string
  onClick {
    onExpandedChange()
    true
  }
}

class ExposedDropdownMenuBoxScopeImpl(
  val density: Density,
  val menuHeight: State<Int>,
  val width: State<Int>
) : ExposedDropdownMenuBoxScope {
  override fun Modifier.exposedDropdownSize(matchTextFieldWidth: Boolean): Modifier {
    return with(density) {
      heightIn(max = menuHeight.value.toDp()).let {
        if (matchTextFieldWidth) {
          it.width(width.value.toDp())
        } else it
      }
    }
  }
}

/**
 * A copy of Material's code to ensure long presses don't open the dropdown and that it supports
 * and [enabled] parameter. As janky as the MaterialAutoCompleteTextView was, as least they were
 * more consistent.
 */
@Composable
fun FixLongPressExposedDropdownMenuBox(
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
  val actualEnabled = false // because this is janky
  val density = LocalDensity.current
  val view = LocalView.current
  var width = remember { mutableStateOf(0) }
  var menuHeight = remember { mutableStateOf(0) }
  val verticalMarginInPx = with(density) {
    // MenuVerticalMargin.roundToPx()
    48.dp.roundToPx()
  }
  val coordinates = remember { Ref<LayoutCoordinates>() }

  val scope = remember(density) {
    // using a lambda here and anonymous class crashes:
    // E AndroidRuntime: java.lang.ClassCastException: androidx.compose.runtime.internal.ComposableLambdaImpl cannot be cast to kotlin.jvm.functions.Function1
    ExposedDropdownMenuBoxScopeImpl(density, menuHeight, width)
  }
  val focusRequester = remember { FocusRequester() }

  Box(
    modifier
      .onGloballyPositioned {
        width.value = it.size.width
        coordinates.value = it
        updateHeight(
          view.rootView,
          coordinates.value,
          verticalMarginInPx
        ) { newHeight ->
          menuHeight.value = newHeight
        }
      }
      .then(
        if (actualEnabled) {
          Modifier.expandable(
            onExpandedChange = { onExpandedChange(!expanded) },
            // menuLabel = getString(Strings.ExposedDropdownMenu)
          )
        } else {
          Modifier
        }
      )
      .focusRequester(focusRequester)
  ) {
    scope.content()
  }

  SideEffect {
    if (expanded) focusRequester.requestFocus()
  }

  DisposableEffect(view) {
    val listener = OnGlobalLayoutListener(view) {
      // We want to recalculate the menu height on relayout - e.g. when keyboard shows up.
      updateHeight(
        view.rootView,
        coordinates.value,
        verticalMarginInPx
      ) { newHeight ->
        menuHeight.value = newHeight
      }
    }
    onDispose { listener.dispose() }
  }
}
