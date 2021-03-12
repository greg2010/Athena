package org.kys.athena.components

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L._
import org.kys.athena.components.common._
import org.kys.athena.components.common.HistoryMenu
import org.kys.athena.riot.api.dto.common.Platform
import org.scalajs.dom.MouseEvent
import scala.math._
import org.kys.athena.util.SearchHistoryManager
import org.kys.athena.components.common.FocusCapturer._
import org.kys.athena.util.assets.AssetLoader
import org.kys.athena.components.common.AlephEye
import org.kys.athena.riot.api.dto.common.Platform

object LandingPage {
  def render: HtmlElement = {
    val focusBus = new EventBus[EventFired]
    div(cls := "flex flex-col items-center container-md flex-grow justify-center",
        AlephEye(),
        span(fontFamily := "heorotregular",
             fontSize := "6rem",
             color := "#780522",
             lineHeight := "0.5",
             cls := "pt-2 pb-3", "Athena"),
        span(cls := "font-bold pb-4", "A solo queue companion"),
        FocusCapturer(
          focusBus.writer,
          cls := "flex flex-col items-center justify-center " +
                 "border shadow-lg border-gray-500 rounded-lg bg-white w-11/12 lg:w-8/12 " +
                 "p-1 divide-y divide-gray-500",
          SearchBar("", Platform.NA, cls := "w-full pb-1"),
          div(
            cls := "w-full",
            HistoryMenu(Some("p-1 font-sans"),
                       cls := "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 justify-center pt-1 gap-1"),
            cls <-- focusBus.events.delay(100).toSignal(FocusOut)
                            .combineWith(SearchHistoryManager.historySignal.map(_.length)).map {
              case (FocusOut, _) => "hidden"
              case (_, 0) => "hidden"
              case _ => ""
           }
          )))
  }
}


