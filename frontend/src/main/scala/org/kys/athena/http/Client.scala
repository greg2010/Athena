package org.kys.athena.http

import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import org.kys.athena.http.errors._
import org.kys.athena.http.models.current.OngoingGameResponse
import org.kys.athena.http.models.premade.PremadeResponse
import org.kys.athena.riot.api.RequestError
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.riot.api.dto.ddragon.champions.Champions
import org.kys.athena.riot.api.dto.ddragon.runes.RuneTree
import org.kys.athena.riot.api.dto.ddragon.summonerspells.SummonerSpells
import org.kys.athena.util.{CacheManager, Config}
import sttp.client3._
import sttp.client3.circe._
import sttp.model.StatusCode
import zio.{IO, UIO}

import java.util.UUID
import scala.concurrent.duration.Duration


object Client {
  private def liftErrors[T](r: Response[Either[RequestError, T]]): IO[BackendApiError, T] = {
    r.body match {
      case Left(HttpError(_, statusCode)) => {
        statusCode match {
          case StatusCode.NotFound => {
            IO.fail(NotFoundError("404: Not Found"))
          }
          case StatusCode.BadRequest => {
            IO.fail(BadRequestError("Bad request"))
          }
          case code => {
            IO.fail(InternalServerError(s"Unknown error with code $code"))
          }
        }
      }
      case Left(DeserializationException(errDesc, ex)) => {
        IO.fail(InternalServerError(s"Failed to parse server response, error=${errDesc}"))
      }
      case Right(resp) => {
        IO.succeed(resp)
      }
    }
  }

  private val cb = TaskFetchBackend()

  private def fetchAndLift[T](req: RequestT[Identity, Either[RequestError, T], Any])
  : IO[BackendApiError, T] = {
    UIO.effectTotal(scribe.info(s"Sending request to url=${req.uri}")).zipRight {
      cb.send(req)
        .mapError { err =>
          InternalServerError(s"Failed to fetch response from server, error=${err.getMessage}")
        }
        .flatMap(liftErrors)
    }
  }

  private def fetchCachedAndLift[T](req: RequestT[Identity, Either[RequestError, T], Any],
                                    cacheFor: Duration = Duration.Inf)
                                   (implicit e: Encoder[T], d: Decoder[T]): IO[BackendApiError, T] = {
    val uri = req.uri.toString()
    CacheManager.get[T](uri).catchAll { err =>
      scribe.warn(s"Failed to fetch from cache with error=${err}")
      UIO.none
    }.flatMap {
      case Some(res) => IO.succeed(res)
      case _ =>
        for {
          resp <- fetchAndLift(req)
          _ <- CacheManager.set(uri, resp, cacheFor).catchAll { err =>
            scribe.warn(s"Failed to set in cache with error=${err}")
            UIO.unit
          }
        } yield resp
    }
  }

  def fetchOngoingGameByName(realm: Platform, name: String)
                            (debug: Boolean = false): IO[BackendApiError, OngoingGameResponse] = {
    val url = if (!debug)
                uri"${Config.BACKEND_API_URL}/current/by-summoner-name/${realm.entryName}/$name?fetchGroups=true"
              else uri"http://localhost:8080/sampleongoing.json"
    val q   = basicRequest
      .get(url)
      .response(asJson[OngoingGameResponse])
    fetchAndLift(q)
  }

  def fetchGroupsByUUID(uuid: UUID)(debug: Boolean = false): IO[BackendApiError, PremadeResponse] = {
    val url = if (!debug)
                uri"${Config.BACKEND_API_URL}/current/by-uuid/${uuid}/groups"
              else uri"http://localhost:8080/samplepremades.json"
    val q   = basicRequest.get(url)
      .response(asJson[PremadeResponse])
    fetchAndLift(q)
  }

  def fetchGroupsByName(realm: Platform, name: String)(debug: Boolean = false): IO[BackendApiError, PremadeResponse] = {
    val url = if (!debug)
                uri"${Config.BACKEND_API_URL}/current/by-summoner-name/${realm.entryName}/$name/groups"
              else uri"http://localhost:8080/samplepremades.json"
    val q   = basicRequest.get(url)
      .response(asJson[PremadeResponse])
    fetchAndLift(q)
  }

  def fetchCachedDDragonChampion(): IO[BackendApiError, Champions] = {
    val url = uri"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/data/${Config.LOCALE}/champion.json"
    val q   = basicRequest.get(url).response(asJson[Champions])
    fetchCachedAndLift(q)

  }

  def fetchCachedDDragonRunes(): IO[BackendApiError, List[RuneTree]] = {
    val url = uri"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/data/${Config.LOCALE}/runesReforged.json"
    val q   = basicRequest.get(url).response(asJson[List[RuneTree]])
    fetchCachedAndLift(q)
  }

  def fetchCachedDDragonSummoners(): IO[BackendApiError, SummonerSpells] = {
    val url = uri"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/data/${Config.LOCALE}/summoner.json"
    val q   = basicRequest.get(url).response(asJson[SummonerSpells])
    fetchCachedAndLift(q)
  }
}
