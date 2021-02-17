package org.kys.athena.util


import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.kys.athena.util.errors.{CacheError, CastError, UnknownError}
import org.scalajs.dom.window
import zio.IO

import scala.concurrent.duration._
import scala.scalajs.js.Date
import scala.util.Try


object CacheManager {
  private case class Codec[T](data: T, expiresAt: Option[Double])

  def get[T](key: String)(implicit d: Decoder[T]): IO[CacheError, Option[T]] = {
    IO.fromEither(getSync(key))
  }

  def set[T](key: String, data: T, cacheFor: Duration = Duration.Inf)(implicit e: Encoder[T]): IO[CacheError, Unit] = {
    IO.fromEither(setSync(key, data, cacheFor))
  }

  def remove[T](key: String): IO[UnknownError, Unit] = {
    IO.fromEither(removeSync(key))
  }

  def setSync[T](key: String, data: T, cacheFor: Duration = Duration.Inf)
                (implicit e: Encoder[T]): Either[CacheError, Unit] = {
    val d           = if (cacheFor == Duration.Inf) None else Some(Date.now() + cacheFor.toMillis)
    val encodedData = Codec(data, d).asJson.noSpaces
    Try(window.localStorage.setItem(key, encodedData)).toEither.left.map(UnknownError)
  }

  def removeSync[T](key: String): Either[UnknownError, Unit] = {
    Try(window.localStorage.removeItem(key)).toEither.left.map(UnknownError)
  }

  def getSync[T](key: String)
                (implicit d: Decoder[T]): Either[CacheError, Option[T]] = {

    def decodeStr(str: String): Either[CacheError, Option[T]] = {

      decode[Codec[T]](str).fold(err => Left(CastError(s"Failed to decode cache with message=${err.getMessage}")), {
        case Codec(data, None) => Right(Some(data))
        case Codec(data, Some(expiresAt)) if expiresAt > Date.now() => Right(Some(data))
        case Codec(_, Some(_)) =>
          window.localStorage.removeItem(key)
          Right(None)
      })
    }

    Try(window.localStorage.getItem(key)).toEither.left.map(UnknownError) match {
      case Left(_) => Right(None)
      case Right(v) => decodeStr(v)
    }
  }
}
