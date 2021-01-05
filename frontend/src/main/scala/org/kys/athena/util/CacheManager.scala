package org.kys.athena.util


import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.kys.athena.util.errors.{CacheError, CastError, UnknownError}
import org.scalajs.dom.window
import zio.{IO, UIO}

import scala.concurrent.duration._
import scala.scalajs.js.Date


object CacheManager {
  private case class Codec[T](data: T, expiresAt: Option[Double])

  def get[T](key: String)(implicit d: Decoder[T]): IO[CacheError, Option[T]] = {
    val attemptFetch = IO.effect(window.localStorage.getItem(key)).bimap(UnknownError, Option(_))

    def decodeStr(str: String): IO[CacheError, Option[T]] = {
      decode[Codec[T]](str) match {
        case Right(Codec(data, None)) => UIO.some(data)
        case Right(Codec(data, Some(expiresAt))) if expiresAt > Date.now() => UIO.some(data)
        case Right(Codec(_, Some(_))) => {
          UIO.effectTotal(window.localStorage.removeItem(key)).as(None)
        }
        case Left(v) => {
          IO.fail(CastError(s"Failed to decode cache with message=${v.getMessage}"))
        }
      }
    }

    attemptFetch.flatMap {
      case Some(s) => decodeStr(s)
      case None => IO.none
    }
  }

  def set[T](key: String, data: T, cacheFor: Duration = Duration.Inf)(implicit e: Encoder[T]): IO[CacheError, Unit] = {
    val d           = if (cacheFor == Duration.Inf) None else Some(Date.now() + cacheFor.toMillis)
    val encodedData = Codec(data, d).asJson.noSpaces

    IO.effect(window.localStorage.setItem(key, encodedData)).mapError(UnknownError)
  }
}
