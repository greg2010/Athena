package org.kys.athena.components.common

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.kys.athena.http.dd.CombinedDD
import org.scalajs.dom.html.Image


object ChampionIcon {
  def apply(id: Long, size: Int, dd: CombinedDD, mods: Modifier[HtmlElement]*): ReactiveHtmlElement[Image] = {
    val champObj = dd.championById(id)
    val url      = dd.championUrl(champObj)
    ImgSized(url, size, Some(size), mods)
  }

}
