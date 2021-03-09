package org.kys.athena.components.common

import com.raquo.laminar.api.L._
import org.kys.athena.routes.OngoingRoute
import org.kys.athena.util.SearchHistoryManager

object HistoryMenu {
  def apply(dropdownElemCls: Option[String], mods: Modifier[HtmlElement]*): HtmlElement = {
    implicit val ordering: Ordering[SearchHistoryManager.HistorySummoner] =
      Ordering.by[SearchHistoryManager.HistorySummoner, Boolean](!_.isStarred).orElseBy(_.savedAt * -1)

    div(
      children <-- SearchHistoryManager.historySignal.map { l =>
        l.sorted.map { lsd =>
          div(
            cls := "flex items-center justify-between",
            cls := dropdownElemCls.fold("")(identity),
            Link(OngoingRoute(lsd.platform, lsd.name),span(s"${lsd.platform}/${lsd.name}")),
            div(
              cls := "flex flex-row",
              if (!lsd.isStarred) {
                button(
                  img(
                    src := "/icons/star.svg", // TODO: replace with AssetLoader when merged
                    cls := "h-5"),
                    onClick.preventDefault --> (_ => SearchHistoryManager.star(lsd.name, lsd.platform)))
              } else {
                button(
                  img(
                    src := "/icons/star-filled.svg", // TODO: replace with AssetLoader when merged
                    cls := "h-5"),
                    onClick.preventDefault --> (_ => SearchHistoryManager.unstar(lsd.name, lsd.platform)))
              },
              button(
                cls := "ml-1",
                img(src := "/icons/trash.svg", cls := "h-5"), // TODO: replace with AssetLoader when merged
                onClick.preventDefault --> (_ => SearchHistoryManager.removeSearch(lsd.name, lsd.platform))))
          )
        }
      }, mods)
    }
}