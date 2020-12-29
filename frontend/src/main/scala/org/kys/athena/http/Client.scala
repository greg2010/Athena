package org.kys.athena.http

import cats.effect.{Concurrent, ContextShift, IO}
import io.circe
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.{Decoder, Encoder}
import org.kys.athena.http.models.current.OngoingGameResponse
import org.kys.athena.http.models.premade.PremadeResponse
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.riot.api.dto.ddragon.champions.Champions
import org.kys.athena.riot.api.dto.ddragon.runes.RuneTree
import org.kys.athena.riot.api.dto.ddragon.summonerspells.SummonerSpells
import org.kys.athena.util.exceptions.{NotFoundException, RiotException}
import org.kys.athena.util.{CacheManager, Config}
import org.scalajs.dom.window
import sttp.client3._
import sttp.client3.circe._
import sttp.model.StatusCode

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue


object Client {
  private def liftErrors[T](r: Response[Either[ResponseException[String, circe.Error], T]])
  : IO[T] = {
    r.body match {
      case Left(HttpError(body, statusCode)) => {
        statusCode match {
          case StatusCode.NotFound => {
            IO.raiseError(NotFoundException("404: Not Found"))
          }
          case code => {
            val maybeReason: Option[String] = parse(body)
              .flatMap(_.hcursor.downField("status").get[String]("message"))
              .toOption
            IO.raiseError(RiotException(code.code, maybeReason))
          }
        }
      }
      case Left(DeserializationException(errDesc, ex)) => {
        scribe.error(s"Failed to parse response. error=${errDesc}", ex)
        IO.raiseError(ex)
      }
      case Right(resp) => {
        IO.pure(resp)
      }
    }
  }

  implicit val cs: ContextShift[IO] = IO.contextShift(queue)
  private val cb = CatsFetchBackend()(implicitly[Concurrent[IO]], cs)

  private def fetchAndLift[T](req: RequestT[Identity, Either[ResponseException[String, circe.Error], T], Any]): IO[T] = {
    cb.send(req).flatMap(r => liftErrors(r))
  }

  private def fetchCachedAndLift[T](req: RequestT[Identity, Either[ResponseException[String, circe.Error], T], Any],
                                    cacheFor: Duration = Duration.Inf)(implicit e: Encoder[T], d: Decoder[T]): IO[T] = {
    val uri = req.uri.toString()
    CacheManager.get[T](uri).flatMap {
      case Some(res) => IO.pure(res)
      case _ =>
        for {
          resp <- cb.send(req)
          lifted <- liftErrors(resp)
          _ <- CacheManager.set(uri, lifted, cacheFor)
        } yield lifted
    }
  }

  def fetchOngoingGameByName(realm: Platform, name: String)(debug: Boolean = false): IO[OngoingGameResponse] = {
    val url = if (!debug)
                uri"${Config.BACKEND_API_URL}/current/by-summoner-name/${realm.entryName}/$name?fetchGroups=true"
              else uri"http://localhost:8080/sampleongoing.json"
    val q   = basicRequest
      .get(url)
      .response(asJson[OngoingGameResponse])
    fetchAndLift(q)
  }

  def fetchGroupsByUUID(uuid: UUID)(debug: Boolean = false): IO[PremadeResponse] = {
    val url = if (!debug)
                uri"${Config.BACKEND_API_URL}/current/by-uuid/${uuid}/groups"
              else uri"http://localhost:8080/samplepremades.json"
    val q   = basicRequest.get(url)
      .response(asJson[PremadeResponse])
    fetchAndLift(q)
  }

  def fetchGroupsByName(realm: Platform, name: String)(debug: Boolean = false): IO[PremadeResponse] = {
    val url = if (!debug)
                uri"${Config.BACKEND_API_URL}/current/by-summoner-name/${realm.entryName}/$name/groups"
              else uri"http://localhost:8080/samplepremades.json"
    val q   = basicRequest.get(url)
      .response(asJson[PremadeResponse])
    fetchAndLift(q)
  }

  def fetchCachedDDragonChampion(): IO[Champions] = {
    val url = uri"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/data/${Config.LOCALE}/champion.json"
    val q   = basicRequest.get(url).response(asJson[Champions])
    fetchCachedAndLift(q)

  }

  def fetchCachedDDragonRunes(): IO[List[RuneTree]] = {
    val url = uri"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/data/${Config.LOCALE}/runesReforged.json"
    val q   = basicRequest.get(url).response(asJson[List[RuneTree]])
    fetchCachedAndLift(q)
  }

  def fetchCachedDDragonSummoners(): IO[SummonerSpells] = {
    val url = uri"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/data/${Config.LOCALE}/summoner.json"
    val q   = basicRequest.get(url).response(asJson[SummonerSpells])
    fetchCachedAndLift(q)
  }
}
