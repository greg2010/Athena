package org.kys.athena.components.ongoing

import com.raquo.laminar.api.L._
import org.kys.athena.routes.OngoingRoute
import org.scalajs.dom


object OngoingError {
  def render(p: OngoingRoute, refreshCb: () => Unit) = {
    div(
      cls := s"flex flex-col items-center p-4",
      img(
        src := "/images/amumu_error.png"
        ),
      span(
        cls := "text-xl mt-4",
        "Server error occurred."),
      button(
        cls := "bg-gray-300, border border-gray-500 rounded-lg p-2 mt-4",
        "Retry",
        onClick --> Observer[dom.MouseEvent](onNext = _ => refreshCb())))
  }
}
