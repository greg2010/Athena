package org.kys.athena.components


import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.kys.athena.http.DData
import org.scalajs.dom.raw.HTMLElement


object UggLink {
  def apply(id: Long, dd: DData, mods: Modifier[HtmlElement]*): ReactiveHtmlElement[HTMLElement] = {
    dd.championById(id) match {
      case Some(value) => a(href := s"https://u.gg/lol/champions/${value.name}", target := "_blank", mods)
      case None => div(mods)
    }
  }
}