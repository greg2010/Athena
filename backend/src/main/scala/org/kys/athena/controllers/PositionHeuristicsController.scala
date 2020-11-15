package org.kys.athena.controllers

import cats.effect.IO
import cats.implicits._
import io.circe
import io.circe.{Decoder, KeyDecoder, KeyEncoder}
import sttp.client._
import sttp.client.circe._
import io.circe.generic.auto._
import org.kys.athena.api.backends.CombinedSttpBackend
import org.kys.athena.api.dto.common.{GameQueueTypeEnum, SummonerSpellsEnum}
import org.kys.athena.data.{OngoingGameInfo, PositionEnum}
import org.kys.athena.http.models.InGameSummoner
import sttp.model.Uri


class PositionHeuristicsController(combinedSttpBackend: CombinedSttpBackend[IO, Any]) {
  private case class ChampionRates(data: Map[Int, Map[PositionEnum, ChampionRolePlayrate]])
  private case class ChampionRolePlayrate(playRate: Double, winRate: Double, banRate: Double)
  private case class PositionProbability(position: PositionEnum, summonerId: String, probability: Double)


  private lazy val playRates: IO[ChampionRates] = {
    implicit val PositionKeyDecoder: KeyDecoder[PositionEnum] = (key: String) => PositionEnum.withNameOption(key)
    val url = uri"http://cdn.merakianalytics.com/riot/lol/resources/latest/en-US/championrates.json"
    val req = basicRequest.get(url).response(asJson[ChampionRates])
    combinedSttpBackend.sendCached(req).flatMap { r =>
      r.body match {
        case Right(re) => IO.pure(re)
        case Left(err) => IO.raiseError(err)
      }
    }
  }

  def estimatePositions(ongoingGameInfo: OngoingGameInfo, team: Set[InGameSummoner]): IO[Map[PositionEnum, String]] = {
    // Reject non-summoner's rift games
    ongoingGameInfo.gameQueueId match {
      case q if q.in(GameQueueTypeEnum.SummonersRiftBlind,
                     GameQueueTypeEnum.SummonersRiftDraft,
                     GameQueueTypeEnum.SummonersRiftSoloRanked,
                     GameQueueTypeEnum.SummonersRiftFlexRanked,
                     GameQueueTypeEnum.SummonersRiftClash) => {
        playRates.map { playRate =>
          team.toList.permutations.map(PositionEnum.values.zip(_)).toList.map { perm =>
            val score = perm.map {
              case (posn, sum) =>
                playRate.data.get(sum.championId.toInt).flatMap(_.get(posn)) match {
                  case Some(p) => p.playRate
                  case None => 0D
                }
            }.sum
            (perm.toMap.view.mapValues(_.summonerId), score)
          }.maxBy(_._2)._1.toMap
        }
      }
      case _ => IO.pure(Map())
    }
  }
}
