package org.kys.athena.components


import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.kys.athena.riot.api.dto.common.Platform
import org.scalajs.dom.raw.HTMLElement


object SummonerLink {

  def apply(summonerName: String,
            platform: Platform, child: ReactiveHtmlElement[HTMLElement]
           ): ReactiveHtmlElement[HTMLElement]
  = {
    platform match {
      case Platform.KR =>
      {
        a(href := s"https://www.op.gg/summoner/userName=${summonerName}", target := "_blank", child)
      }
      case otherRegions =>
        a(href := s"https://${otherRegions}.op.gg/summoner/userName=${summonerName}", target := "_blank", child)
    }
  }





}
