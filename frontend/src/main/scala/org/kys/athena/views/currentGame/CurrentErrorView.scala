package org.kys.athena.views.currentGame

import com.raquo.laminar.api.L._
import org.kys.athena.pages.CurrentGamePage
import org.scalajs.dom


object CurrentErrorView {
  def render(p: CurrentGamePage, refreshCb: () => Unit) = {
    div(
      cls := s"flex flex-col items-center p-8",
      img(
        src := "/amumu_error.png"
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
