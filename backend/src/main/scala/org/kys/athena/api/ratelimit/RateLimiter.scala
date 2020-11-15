package org.kys.athena.api.ratelimit

import cats.data.NonEmptyList
import cats.implicits._
import cats.effect.{Concurrent, ConcurrentEffect, Resource, Timer}



class RateLimiter[F[_]](limiterList: NonEmptyList[TokenBucket[F]])(implicit F: Concurrent[F]) {

  def execute[A](job: F[A]): F[A] = {
    limiterList match {
      case NonEmptyList(head, tail) =>
        val awaitF = tail.map { l =>
          // Awaiting for all _other_ rate limiters
          l.execute(F.unit)
        }.sequence
        F.flatMap(awaitF) { _ =>
          // Finally, run our job
          head.execute(job)
        }
    }
  }

}

object RateLimiter {
  // Just a nice constructor of RateLimiter
  def start[F[_]](limitList: NonEmptyList[RateLimit])
                 (implicit F: ConcurrentEffect[F], timer: Timer[F]): Resource[F, RateLimiter[F]] = {
    limitList.map(TokenBucket.start(_)).sequence.map(new RateLimiter(_))
  }
}
