package org.kys.athena.modules

import com.github.blemale.scaffeine.Scaffeine
import zio._
import zio.macros.accessible

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

@accessible
object CacheModule {
  type CacheModule = Has[Service]

  trait CacheError extends Throwable

  case class CastError(message: String) extends CacheError
  case class UnknownError(err: Throwable) extends CacheError

  trait Service {
    def put[T](key: String, v: T): IO[CacheError, Unit]
    def get[T](key: String)(implicit evT: ClassTag[T]): IO[CacheError, Option[T]]
  }

  val live = (for {
      config <- ConfigModule.loaded
      cache <- Task.effect {
        Scaffeine().recordStats()
          .expireAfterWrite(FiniteDuration.apply(config.cacheRiotRequestsFor, TimeUnit.SECONDS))
          .maximumSize(config.cacheRiotRequestsMaxCount)
          .build[String, Serializable]()
      }.orDie
    } yield new Service {
      override def put[T](key: String, v: T): IO[CacheError, Unit] = {
        IO.effect(cache.put(key, v.asInstanceOf[Serializable])).mapError(e => UnknownError(e))
      }

      override def get[T](key: String)(implicit ev: ClassTag[T]): IO[CacheError, Option[T]] = {
        ZIO.effect(cache.getIfPresent(key)).flatMap(r => Task.effect(r.map(_.asInstanceOf[T]))).mapError {
          case _: ClassCastException =>
            CastError(s"Failed to cast to ${implicitly[ClassTag[T]].runtimeClass.getSimpleName}")
          case e => UnknownError(e)
        }
      }
    }).toLayer
}
