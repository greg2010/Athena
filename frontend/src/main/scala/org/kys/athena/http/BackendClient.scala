package org.kys.athena.http

import org.kys.athena.http.errors._
import org.kys.athena.http.models.current.OngoingGameResponse
import org.kys.athena.http.models.pregame.PregameResponse
import org.kys.athena.http.models.premade.{PlayerGroup, PremadeResponse}
import org.kys.athena.http.routes.Endpoints
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.datastructures.Config
import sttp.client3._
import sttp.model.Uri
import sttp.tapir.{DecodeResult, Endpoint}
import zio.{IO, UIO}
import sttp.tapir.client.sttp.SttpClientInterpreter

import java.util.UUID


object BackendClient {

  private def liftErrors[T](r: DecodeResult[Either[BackendApiError, T]]): IO[BackendApiError, T] = {
    r match {
      case _: DecodeResult.Failure => IO.fail(InternalServerError(s"Unknown decoding error"))
      case DecodeResult.Value(Left(err)) => IO.fail(err)
      case DecodeResult.Value(Right(res)) => IO.succeed(res)
    }
  }

  private val cb = TaskFetchBackend()

  private def fetchAndLift[T](req: Request[DecodeResult[Either[BackendApiError, T]], Any])
  : IO[BackendApiError, T] = {
    UIO.effectTotal(scribe.info(s"Sending request to url=${req.uri}")).zipRight {
      cb.send(req).mapError { err =>
        InternalServerError(s"Failed to fetch response from server, error=${err.getMessage}")
      }.flatMap(r => liftErrors(r.body))
    }
  }

  private val debug = Config.USE_FAKE_DATA match {
    case "true" => true
    case _ => false
  }

  val serverBaseUrl: Option[Uri] = Some(uri"${Config.BACKEND_API_URL}")

  def interpret[I, E, O](endpoint: Endpoint[I, E, O, Any]): I => Request[DecodeResult[Either[E, O]], Any] = {
    SttpClientInterpreter.toRequest(endpoint, serverBaseUrl)
  }

  def fetchOngoingGameByName(realm: Platform, name: String): IO[BackendApiError, OngoingGameResponse] = {
    val q = interpret(Endpoints.currentGameByName)
    fetchAndLift(q((realm, name, Some(true), None)))
  }

  def fetchGroupsByUUID(uuid: UUID): IO[BackendApiError, PremadeResponse] = {
    val q = interpret(Endpoints.currentGameGroupsByUUID)
    fetchAndLift(q((uuid, None)))
  }

  def fetchGroupsByName(realm: Platform, name: String): IO[BackendApiError, PremadeResponse] = {
    val q = interpret(Endpoints.currentGameGroupsByName)
    fetchAndLift(q((realm, name, None)))
  }

  def fetchPregameGameByName(realm: Platform, names: List[String]): IO[BackendApiError, PregameResponse] = {
    val q = interpret(Endpoints.pregameByName)
    fetchAndLift(q((realm, names.toSet, Some(true), None)))
  }

  def fetchPregameGroupsByUUID(uuid: UUID): IO[BackendApiError, Set[PlayerGroup]] = {
    val q = interpret(Endpoints.pregameGameGroupsByUUID)
    fetchAndLift(q((uuid, None)))
  }

  def fetchPregameGroupsByName(realm: Platform, names: List[String]): IO[BackendApiError, Set[PlayerGroup]] = {
    val q = interpret(Endpoints.pregameGroupsByName)
    fetchAndLift(q((realm, names.toSet, None)))
  }
}
