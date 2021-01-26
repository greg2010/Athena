package org.kys.athena.components

import com.raquo.laminar.api.L._
import org.kys.athena.App
import org.kys.athena.pages.CurrentGamePage
import org.kys.athena.riot.api.dto.common.Platform
import org.scalajs.dom
import org.scalajs.dom.Event


object SearchBar {
  private val summoner: Var[String]   = Var[String]("")
  private val platform: Var[Platform] = Var[Platform](Platform.NA)
  private val formObserver: Observer[Event] =
    Observer[dom.Event](onNext = _ => (platform.now(), summoner.now()) match {
      case (_, "") => ()
      case (p, s) => App.pushState(CurrentGamePage(p, s))
    })

  def render(widthClasses: String, heightClasses: String = "h-12", textClasses: String = "text-xl"): HtmlElement = {
    form(cls := "appearance-none border shadow-lg border-gray-300 text-gray-darker " +
                s"$textClasses border rounded-md px-3 py-1 flex items-center bg-white $widthClasses",
         input(placeholder := "Enter a summoner name", cls := s"flex-grow min-w-0 $heightClasses",
               inContext(thisNode => onChange.mapTo(thisNode.ref.value) --> summoner)),
         select(cls := "appearance-none px-1",
                optGroup(cls := "text-md", Platform.values.map(renderPlatformOption)),
                inContext(
                  thisNode => {
                    onChange.mapTo(Platform.withNameOption(thisNode.ref.value).getOrElse(Platform.NA)) -->
                    platform
                  })),
         img(src := "/icons/search.svg", width := "24px", height := "auto", onClick --> formObserver),
         onSubmit.preventDefault --> formObserver)
  }

  private def renderPlatformOption(platform: Platform): HtmlElement = {
    val optionName = platform.entryName.filterNot(_.isDigit)
    option(value := platform.entryName, optionName)
  }
}
