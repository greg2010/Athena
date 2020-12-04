package org.kys.athena.util

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.scalajs.dom.window

import scala.concurrent.duration._
import scala.scalajs.js.Date


object CacheManager {
  private case class Codec[T](data: T, expiresAt: Option[Double])

  def get[T](key: String)(implicit d: Decoder[T]): IO[Option[T]] = {
    IO.delay(window.localStorage.getItem(key)).map(Option(_)).map { s =>
      s.flatMap { str =>
        decode[Codec[T]](str) match {
          case Right(Codec(data, None)) => Some(data)
          case Right(Codec(data, Some(expiresAt))) if expiresAt > Date.now() => Some(data)
          case Right(Codec(_, Some(_))) => {
            window.localStorage.removeItem(key)
            None
          }
          case Left(err) => {
            scribe.warn(s"Fetch from cache failed, key=${key} err=${err}")
            None
          }
        }
      }
    }
  }

  def set[T](key: String, data: T, cacheFor: Duration = Duration.Inf)(implicit e: Encoder[T]): IO[Unit] = {
    val d = if (cacheFor == Duration.Inf) None else Some(Date.now() + cacheFor.toMillis)
    val encodedData = Codec(data, d).asJson.noSpaces

    IO.delay(window.localStorage.setItem(key, encodedData))
  }
}
