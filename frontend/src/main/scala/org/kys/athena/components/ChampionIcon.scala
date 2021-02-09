package org.kys.athena.components

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.kys.athena.http.DData
import org.scalajs.dom.html.Image


object ChampionIcon {
  def apply(id: Long, size: Int, dd: DData, mods: Modifier[HtmlElement]*): ReactiveHtmlElement[Image] = {
    val champObj = dd.championById(id)
    val url      = dd.championUrl(champObj)
    ImgSized(url, size, Some(size), mods)
  }

}
