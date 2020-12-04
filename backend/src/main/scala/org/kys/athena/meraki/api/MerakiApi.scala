package org.kys.athena.meraki.api

import io.circe
import io.circe.KeyDecoder
import io.circe.generic.auto._
import org.kys.athena.http.models.current.PositionEnum
import org.kys.athena.meraki.api.dto.ChampionRates
import sttp.client3._
import sttp.client3.circe._


class MerakiApi {
  def playRates: RequestT[Identity, Either[ResponseException[String, circe.Error], ChampionRates], Any] = {
    implicit val PositionKeyDecoder: KeyDecoder[PositionEnum] = (key: String) => PositionEnum.withNameOption(key)
    val url = uri"http://cdn.merakianalytics.com/riot/lol/resources/latest/en-US/championrates.json"
    basicRequest.get(url).response(asJson[ChampionRates])
  }
}