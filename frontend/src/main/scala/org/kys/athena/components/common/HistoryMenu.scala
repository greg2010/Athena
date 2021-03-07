package org.kys.athena.components.common

import com.raquo.laminar.api.L._
import org.kys.athena.routes.OngoingRoute
import org.kys.athena.util.SearchHistoryManager

object HistoryMenu {
  def apply(dropdownElemCls: Option[String], mods: Modifier[HtmlElement]*): HtmlElement = {
    div(
      children <-- SearchHistoryManager.historySignal.map { l =>
        l.map { lsd => 
          div(
            cls := "flex items-center justify-between",
            Link(OngoingRoute(lsd.platform, lsd.name),span(cls := dropdownElemCls.fold("")(identity), s"${lsd.platform}/${lsd.name}")),
            button(
              img(src := "/icons/trash.svg", cls := "h-4"),
              onClick.preventDefault.mapTo(lsd) --> SearchHistoryManager.removeSearch _))
        }
      }, mods)
    }
}