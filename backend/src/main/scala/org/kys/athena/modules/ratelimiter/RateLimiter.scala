package org.kys.athena.modules.ratelimiter

import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.riot.api.{RateLimitInitial, ResponseLimitStatus}
import zio._
import zio.clock.Clock
import zio.console.Console
import zio.stm._


trait RateLimiter {
  def executePlatform[E, A](k: String, p: Platform, j: IO[E, A], rllFunc: A => ResponseLimitStatus): IO[E, A]
}


object RateLimiter {
  def make: ZManaged[Clock with Console, Throwable, RateLimiter] = {
    for {
      runtime <- ZIO.runtime[Clock with Console].toManaged_
      methodRrMap <- TMap.empty[String, TPromise[Nothing, IORateLimiter]].commit.toManaged_
      platformRrMap <- TMap.empty[Platform, TPromise[Nothing, IORateLimiter]].commit.toManaged_
    } yield new RateLimiter {

      def allocateRateLimiter(logKey: String, ll: List[RateLimitInitial]): UIO[IORateLimiter] = {
        for {
          _ <- UIO.effectTotal {
            scribe.debug(s"Allocating new ratelimiter for key=$logKey ll=${ll.mkString(",")}")
          }
          rl <- IORateLimiter.make(ll).reserve.flatMap(_.acquire.provide(runtime.environment))

        } yield rl
      }

      def setMethodPromise(key: String): USTM[TPromise[Nothing, IORateLimiter]] = {
        for {
          pr <- TPromise.make[Nothing, IORateLimiter]
          _ <- methodRrMap.put(key, pr)
        } yield pr
      }

      def setPlatformPromise(platform: Platform): USTM[TPromise[Nothing, IORateLimiter]] = {
        for {
          pr <- TPromise.make[Nothing, IORateLimiter]
          _ <- platformRrMap.put(platform, pr)
        } yield pr
      }

      override def executePlatform[E, A](k: String,
                                         p: Platform,
                                         j: IO[E, A],
                                         rllFunc: A => ResponseLimitStatus): IO[E, A] = {

        val trs: UIO[(Boolean, TPromise[Nothing, IORateLimiter], Boolean, TPromise[Nothing, IORateLimiter])] = (for {
          mRll <- methodRrMap.get(k)
          mf <- mRll.fold(setMethodPromise(k))(tp => STM.succeed(tp))
          pRll <- platformRrMap.get(p)
          pf <- pRll.fold(setPlatformPromise(p))(tp => STM.succeed(tp))
        } yield (mRll.nonEmpty, mf, pRll.nonEmpty, pf)).commit

        for {
          t <- trs
          (mFilled, mPromise, pFilled, pPromise) = t
          res <- (mFilled, pFilled) match {
            case (true, true) => {
              for {
                mm <- mPromise.await.commit
                pp <- pPromise.await.commit
                r <- mm.execute(pp.execute(j))
              } yield r
            }
            case (false, true) => {
              for {
                pp <- pPromise.await.commit
                r <- pp.execute(j)
                rlls = rllFunc(r)
                miorl <- allocateRateLimiter(k, rlls.methodRateLimits)
                _ <- mPromise.succeed(miorl).commit
              } yield r
            }
            case (true, false) => {
              for {
                mm <- pPromise.await.commit
                r <- mm.execute(j)
                rlls = rllFunc(r)
                piorl <- allocateRateLimiter(p.toString, rlls.platformRateLimits)
                _ <- pPromise.succeed(piorl).commit
              } yield r
            }
            case (false, false) => {
              for {
                r <- j
                rlls = rllFunc(r)
                miorl <- allocateRateLimiter(k, rlls.methodRateLimits)
                piorl <- allocateRateLimiter(p.toString, rlls.platformRateLimits)
                _ <- mPromise.succeed(miorl).commit
                _ <- pPromise.succeed(piorl).commit
              } yield r
            }
          }
        } yield res
      }
    }
  }

  val live: ZLayer[Clock with Console, Throwable, Has[RateLimiter]] = RateLimiter.make.toLayer
}