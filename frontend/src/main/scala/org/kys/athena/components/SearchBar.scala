package org.kys.athena.components

import com.raquo.laminar.api.L._
import org.kys.athena.App
import org.kys.athena.pages.CurrentGamePage
import org.kys.athena.riot.api.dto.common.Platform
import org.scalajs.dom
import org.scalajs.dom.Event


object SearchBar {
  def apply(initialSummoner: String = "", initialPlatform: Platform = Platform.NA): HtmlElement = {

    val summoner    : Var[String]     = Var[String](initialSummoner)
    val platform    : Var[Platform]   = Var[Platform](initialPlatform)
    val formObserver: Observer[Event] =
      Observer[dom.Event](onNext = _ => {
        (platform.now(), summoner.now()) match {
          case (_, "") => ()
          case (p, s) => App.pushState(CurrentGamePage(p, s))
        }
      })


    form(cls := "border shadow-lg border-gray-500 text-gray-darker " +
                s"rounded-lg px-3 py-1 flex items-center bg-white",
         input(placeholder := "Enter a summoner name",
               cls := s"flex-grow min-w-0 focus:outline-none appearance-none",
               value <-- summoner.signal,
               inContext(thisNode => onChange.mapTo(thisNode.ref.value) --> summoner)),
         DropdownMenu[Platform](platform.signal.map(_.toString),
                                Platform.values.toList,
                                platform.writer,
                                Some("focus:outline-none text-md"),
                                Some(s"border shadow-lg border-gray-500 p-1 rounded-sm bg-white"),
                                Some("focus:outline-none text-md")).amend(
           cls := s"focus:outline-none appearance-none px-1"
           ),
         img(src := "/icons/search.svg", width := "24px", height := "auto", onClick --> formObserver),
         onSubmit.preventDefault --> formObserver)
  }
}
