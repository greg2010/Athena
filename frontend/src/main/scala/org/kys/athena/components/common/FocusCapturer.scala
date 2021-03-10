package org.kys.athena.components.common

import com.raquo.laminar.api.L._

object  FocusCapturer {
  sealed trait EventFired
  case object FocusIn extends EventFired
  case object FocusOut extends EventFired

  def apply(writer: Observer[EventFired], mods: Modifier[HtmlElement]*): HtmlElement = {
    div(
      onFocus.preventDefault.useCapture.mapTo(FocusIn) --> writer,
      onBlur.preventDefault.useCapture.mapTo(FocusOut) --> writer,
      mods
    )
  }
}
