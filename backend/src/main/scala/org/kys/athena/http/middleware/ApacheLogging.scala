package org.kys.athena.http.middleware

import java.util.Calendar

import cats.data.{Kleisli, OptionT}
import cats.effect.Effect
import com.typesafe.scalalogging.LazyLogging
import org.http4s.headers.`User-Agent`
import org.http4s._
import cats.implicits._


/**
  * [[ApacheLogging]] contains, as the name suggests, custom org.kys.lolassistant.http middleware for our [[org
  * .http4s.server]] server.
  */
object ApacheLogging extends LazyLogging {

  /** Apache log implementation. Backed by [[org.log4s]].
    * Apache log string: `"%h %t %r %>s %b %{Referer}i %{User-agent}i %Dms"`
    * IMPORTANT: %D according to apache docs is time in microseconds. We show milliseconds (more useful).
    *
    * @param req  Information about the request (host, ip, headers, etc)
    * @param resp Information about the response (status code, body size, etc)
    * @param time Time taken to compute the response (in ms)
    * @tparam F [[cats.effect.IO]] */
  private def apacheLoggingImpl[F[_]](req: Request[F], resp: Response[F], time: Long)
                                     (implicit F: Effect[F]): F[Unit] = {
    resp.bodyAsText(resp.charset.getOrElse(Charset.`UTF-8`)).map { body =>
      logger.info(s"${req.remoteAddr.getOrElse("")} " + s"${Calendar.getInstance().getTime} " +
                  s"${req.method} ${req.pathInfo} ${req.httpVersion} " + s"${resp.status.code} " + s"${body.length} " +
                  s"${req.headers.find(_.name == "referer").getOrElse("-")} " +
                  req.headers.get(`User-Agent`).map(ua => s"${ua.renderString} ").getOrElse("") + s"${time}ms")
    }.compile.drain
  }

  private def exceptionLogging(ex: Throwable): Unit = {
    logger.error("Uncaught exception reached middleware", ex)
  }

  /** Apache logging middleware implementation. Defines a function that takes [[HttpService]]
    * and returns [[HttpService]], while doing some logging along the way.
    *
    * @param service [[HttpService]] to be wrapped (actual endpoints)
    * @param F       Auxiliary implicit used by [[cats]] library
    * @tparam F [[cats.effect.IO]]
    * @return [[HttpService]] that contains original service with the logger wrapped around it */
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def apply[F[_]](service: HttpRoutes[F])
                 (implicit F: Effect[F]): HttpRoutes[F] = {
    Kleisli { req: Request[F] =>
      val t0 = System.currentTimeMillis()
      OptionT {
        service(req).value.attempt.flatMap {
          // Case request is valid and response is generated
          case Right(Some(resp)) => F.pure(resp) // Case no response is generated (404)
          case Right(None) => Response.notFoundFor(req) // Case non-200 response is generated (parsing exception, etc)
          case Left(ex: MessageFailure) => exceptionLogging(ex)
            F.pure(ex.toHttpResponse[F](req.httpVersion)) // Case unknown exception is generated (500)
          case Left(ex) => exceptionLogging(ex)
            F.raiseError(ex)
            throw ex
        }.flatMap { resp: Response[F] =>
          val t1 = System.currentTimeMillis()
          apacheLoggingImpl(req, resp, t1 - t0).map(_ => Some(resp))
        }
      }
    }
  }
}
