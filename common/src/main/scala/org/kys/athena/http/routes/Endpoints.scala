package org.kys.athena.http.routes

import org.kys.athena.http.models.current.{OngoingGameResponse, PositionEnum}
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform, SummonerSpellsEnum}
import sttp.tapir._
import sttp.tapir.codec.enumeratum._
import io.circe.generic.auto._
import org.kys.athena.http.errors.{BackendApiError, BadRequestError, InternalServerError, NotFoundError}
import org.kys.athena.http.models.premade.PremadeResponse
import org.kys.athena.riot.api.dto.league.{RankedQueueTypeEnum, TierEnum}
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import sttp.model.StatusCode

import java.util.UUID

trait Endpoints {
  private implicit val schemaForPlatform: Schema[Platform] = Schema.string
  private implicit val schemaForGQTE: Schema[GameQueueTypeEnum] = Schema.string
  private implicit val schemaForPE: Schema[PositionEnum] = Schema.string
  private implicit val schemaForSSE: Schema[SummonerSpellsEnum] = Schema.string
  private implicit val schemaForRQTE: Schema[RankedQueueTypeEnum] = Schema.string
  private implicit val schemaForTE: Schema[TierEnum] = Schema.string
  private implicit val schemaForPM: Schema[Map[PositionEnum, String]] = Schema.string //TODO fix schema

  val defaultErrorCodes = oneOf[BackendApiError](
    statusMapping(StatusCode.NotFound, jsonBody[NotFoundError]),
    statusMapping(StatusCode.BadRequest, jsonBody[BadRequestError]),
    statusMapping(StatusCode.InternalServerError, jsonBody[InternalServerError]))

  val currentGameByName: Endpoint[(Platform, String, Option[Boolean]), BackendApiError, OngoingGameResponse, Any] =
    endpoint.get
      .in("current" / "by-summoner-name")
      .in(path[Platform]("platformId") / path[String]("summonerName"))
      .in(query[Option[Boolean]]("fetchGroups"))
      .out(jsonBody[OngoingGameResponse])
      .errorOut(defaultErrorCodes)
      .summary("Endpoint to get current game info for a player")

  val groupsByName: Endpoint[(Platform, String), BackendApiError, PremadeResponse, Any] =
    endpoint.get
      .in("current" / "by-summoner-name")
      .in(path[Platform]("platformId") / path[String]("summonerName"))
      .in("groups")
      .out(jsonBody[PremadeResponse])
      .errorOut(defaultErrorCodes)
      .summary("Endpoint to get premades in a game for a player")

  val groupsByUUID: Endpoint[UUID, BackendApiError, PremadeResponse, Any] =
    endpoint.get
      .in("current" / "by-uuid")
      .in(path[UUID]("uuid"))
      .in("groups")
      .out(jsonBody[PremadeResponse])
      .errorOut(defaultErrorCodes)
      .summary("Endpoint to get premades in a game for a player by UUID")
}
