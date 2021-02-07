package org.kys.athena.views.currentGame

import org.kys.athena.pages.CurrentGamePage
import com.raquo.laminar.api.L._
import org.kys.athena.components.SearchBar
import org.scalajs.dom


object CurrentNotFoundView {
  def render(p: CurrentGamePage, refreshCb: () => Unit) = {
    div(
      cls := s"flex flex-col items-center p-8",
      img(
        src := "/blitzcrank_logo.png"
        ),
      span(
        cls := "text-xl mt-4",
        "Summoner ",
        b(s"${p.realm.toString}/${p.name}"),
        " is not currently in game."),
      button(
        cls := "bg-gray-300, border border-gray-500 rounded-lg p-2 mt-4",
        "Retry",
        onClick --> Observer[dom.MouseEvent](onNext = _ => refreshCb())),
      span("OR", cls := "font-bold text-lg py-1"),
      SearchBar(p.name, p.realm))
  }
}
