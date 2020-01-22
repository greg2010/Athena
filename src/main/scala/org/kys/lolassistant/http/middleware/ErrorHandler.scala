package org.kys.lolassistant.http.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.Effect
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import org.http4s.dsl.Http4sDsl
import org.http4s._
import org.kys.lolassistant.http.models.ErrorResponse
import org.kys.lolassistant.util.exceptions.{BadRequestException, InconsistentAPIException, NotFoundException, RiotException}
import cats.implicits._

/**
  * Error handling middleware for [[org.http4s]] REST server. It converts various exceptions into http status codes,
  * and returns 500 on unknown exceptions
  */
object ErrorHandler extends LazyLogging {
  def apply[F[_]](service: HttpRoutes[F])
                 (implicit F: Effect[F]): HttpRoutes[F] = Kleisli { req: Request[F] =>
    val dslInstance = Http4sDsl.apply[F]
    import dslInstance._
    import org.http4s.circe.CirceEntityEncoder._
    OptionT {
      service(req).value.attempt.flatMap {
        // Case request is valid and response is generated
        case Right(Some(resp)) => F.pure(resp)
        // Case no response is generated (404)
        case Right(None) => Response.notFoundFor(req)
        // Case non-200 response is generated (parsing exception, etc)
        case Left(ex: MessageFailure) =>
          ex.toHttpResponse[F](req.httpVersion)
        // All other exceptions
        case Left(ex: BadRequestException) =>
          logger.info(s"Generated 400, reason=${ex.reason}")
          BadRequest(ErrorResponse(0, "Bad Request"))
        case Left(ex: NotFoundException) =>
          logger.info(s"Generated 404, reason=${ex.reason}")
          NotFound(ErrorResponse(0, "Not found"))
        case Left(ex: InconsistentAPIException) =>
          logger.error(s"Caught inconsistent API, dtoName=${ex.dtoName} errorDesc=${ex.errorDesc}")
          BadGateway("Riot API Problems")
        case Left(ex: RiotException) =>
          logger.error(s"Caught RiotException, statusCode=${ex.statusCode} errorMessage=${ex.errorMessage}")
          BadGateway("Riot API Problems")
        case Left(ex) =>
          logger.error("Caught unknown exception", ex)
          InternalServerError("Error occurred")
      }.map { resp: Response[F] =>
        Some(resp)
      }
    }
  }
}
