package org.kys.athena.components.common

import com.raquo.laminar.api.L._
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.routes.LandingRoute
import org.kys.athena.util.CSSUtil
import org.kys.athena.datastructures.Config
import org.kys.athena.App
import org.kys.athena.routes.OngoingRoute
import org.kys.athena.components.common.FocusCapturer._


object AppBar {
  def apply(hideSearchBar: Signal[Boolean]): HtmlElement = {
    val focusBus = new EventBus[EventFired]
    val focusSignal = focusBus.events.delay(100).toSignal(FocusCapturer.FocusOut)

    val showSignal = App.routerSignal.combineWith(hideSearchBar.signal).map {
      case (LandingRoute, _) => false
      case (_: OngoingRoute, sig) => !sig
      case _ => true
    }

    val showTitle = App.routerSignal.map {
      case LandingRoute => false
      case _ => true
    }

    nav(cls := "shadow-lg w-full px-3 py-2 flex items-center justify-between h-14",
        backgroundColor := CSSUtil.paletteHeader,
        child <-- showTitle.map {
          case true =>
            Link(LandingRoute,
                 span(cls := "font-medium text-white tracking-wider", fontFamily := "heorotregular", "Athena"))
          case false => div()
        },
        div(
          cls := "flex flex-row h-full",
          a(cls := "mx-2", href := "https://github.com/greg2010/Athena", target := "_blank",
            ImgSized(s"${Config.FRONTEND_URL}/images/gh_logo.png", 40, Some(40))),
          child <-- showSignal.map {
            case true =>
            FocusCapturer(
              focusBus.writer,
              SearchBar(
                "",
                Platform.NA,
                cls := "border shadow-lg rounded-lg bg-white",
                cls <-- focusSignal.map {
                 case FocusIn => "rounded-b-none"
                 case FocusOut => ""
                }),
              HistoryMenu(
                Some("p-1 font-sans"),
                width := "inherit",
                cls := s"relative z-0 bg-white rounded-b-xl border border-gray-500 rounded-t-none",
                display <--focusSignal.map {
                 case FocusIn => "block"
                 case FocusOut => "none"
               }))
            case false => div()
          })
        )
  }
}
