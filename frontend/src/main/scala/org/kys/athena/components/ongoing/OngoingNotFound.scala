package org.kys.athena.components.ongoing

import com.raquo.airstream.eventbus.EventBus
import org.kys.athena.routes.OngoingRoute
import com.raquo.laminar.api.L._
import org.kys.athena.components.common.{HistoryMenu, SearchBar}
import org.scalajs.dom


object OngoingNotFound {
  sealed trait EventFired
  case object FocusIn extends EventFired
  case object FocusOut extends EventFired

  def render(p: OngoingRoute, refreshCb: () => Unit) = {
    val focusBus = new EventBus[EventFired]
    div(
      cls := s"flex flex-col items-center p-4",
      img(src := "/images/blitzcrank_logo.png"),
      span(
        cls := "text-xl mt-4", "Summoner ", b(s"${p.realm.toString}/${p.name}"), " is not currently in game."),
      div(
        cls := "rounded-lg bg-white border border-gray-500 w-5/6 my-4 h-10" +
        "p-1 divide-y divide-gray-500",
        onFocus.preventDefault.useCapture.mapTo(FocusIn) --> focusBus.writer,
        onBlur.preventDefault.useCapture.mapTo(FocusOut) --> focusBus.writer,
        SearchBar(p.name,
                p.realm,
                cls := "",
                onSubmit.preventDefault --> Observer[dom.Event](onNext = _ => refreshCb())),
        HistoryMenu(
               Some("font-sans p-1"),
               cls := "grid grid-cols-1 lg:grid-cols-2 justify-center pt-1 gap-1",
               cls <--focusBus.events.delay(100).toSignal(FocusOut).map{
                 case FocusIn => ""
                 case FocusOut => "hidden"
               })))
  }
}
