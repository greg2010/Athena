package org.kys.athena.modules

import com.github.blemale.scaffeine.Scaffeine
import org.kys.athena.util.errors.{CacheError, CastError, UnknownError}
import zio._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag


trait CacheModule {
  def put[T](key: String, v: T): IO[CacheError, Unit]

  def get[T](key: String)(implicit evT: ClassTag[T]): IO[CacheError, Option[T]]
}


object CacheModule {

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Serializable"))
  val live = (for {
    config <- ConfigModule.loaded
    cache <- Task.effect {
      Scaffeine().recordStats()
        .expireAfterWrite(FiniteDuration.apply(config.cacheRiotRequestsFor, TimeUnit.SECONDS))
        .maximumSize(config.cacheRiotRequestsMaxCount)
        .build[String, Serializable]()
    }.orDie
  } yield new CacheModule {
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

  def get[T](key: String)(implicit evT: ClassTag[T]): ZIO[Has[CacheModule], CacheError, Option[T]] = {
    ZIO.accessM[Has[CacheModule]](_.get.get(key)(evT))
  }

  def put[T](key: String, v: T): ZIO[Has[CacheModule], CacheError, Unit] = {
    ZIO.accessM[Has[CacheModule]](_.get.put(key, v))
  }
}
