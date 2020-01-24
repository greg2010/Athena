package org.kys.athena

import sttp.client.{DeserializationError, Response}
import io.circe.{Error => CError}
import io.circe.generic.auto._
import io.circe.parser._
import org.kys.athena.http.models.ErrorResponse
import sttp.client._
import sttp.client.FetchBackend
import sttp.model.StatusCode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util._



object Main {

  val logger = java.util.logging.Logger.getLogger("main")

  private def liftErrors[T](r: Response[Either[String, String]]): Future[T] = {
    r.body match {
      case Left(str) => {
        r.code match {
          case StatusCode.NotFound => {
            logger.fine(s"Athena api responded with 404")
            Future.failed(new RuntimeException("Athena API responded: Not Found"))
          }
          case code => {
            val maybeReason = decode[ErrorResponse](str).toOption
            logger.warning(s"Got non-200/404 from Athena API: code=${code.code} maybeReason=$maybeReason")
            Future.failed(new RuntimeException(maybeReason.map(_.toString).getOrElse("")))
          }
        }
      }
      case Right(resp) =>
        decode(resp)(CirceEncodableDecoder) match {
          case Left(ex: CError) => {
            logger.severe(s"Got parse error while parsing Athena API response. error=${ex.getMessage}\n" + ex)
            Future.failed(ex)
          }
          case Right(decodedResp) => Future.successful(decodedResp.asInstanceOf[T])
        }
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val sttpBackend = FetchBackend()
    val request = emptyRequest
      .get(uri"http://localhost:8080/premades/by-summoner-name/_?platform=NA1&gameDepth=3")
      //.response(asJson[PremadeResponse])
    val resp = request.send().flatMap(liftErrors)
    //println(resp)
    resp.andThen {
      case r =>
        logger.info("info?" + r.toString)
        logger.severe("error" + r.toString)
    }
  }
}
