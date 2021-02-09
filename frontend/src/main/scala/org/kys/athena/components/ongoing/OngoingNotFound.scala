package org.kys.athena.components.ongoing

import org.kys.athena.routes.OngoingRoute
import com.raquo.laminar.api.L._
import org.kys.athena.components.common.SearchBar
import org.scalajs.dom


object OngoingNotFound {
  def render(p: OngoingRoute, refreshCb: () => Unit) = {
    div(
      cls := s"flex flex-col items-center p-4",
      img(src := "/blitzcrank_logo.png"),
      span(
        cls := "text-xl mt-4", "Summoner ", b(s"${p.realm.toString}/${p.name}"), " is not currently in game."),
      SearchBar(p.name,
                p.realm,
                cls := "rounded-lg bg-white border border-gray-500 w-5/6 my-4 h-10",
                onSubmit.preventDefault --> Observer[dom.Event](onNext = _ => refreshCb())))
  }
}
