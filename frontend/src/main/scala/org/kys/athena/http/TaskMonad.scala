package org.kys.athena.http

import sttp.monad.{Canceler, MonadAsyncError}
import zio._

import scala.util.Try


class TaskMonad extends MonadAsyncError[Task] {
  override def unit[T](t: T): Task[T] = Task.succeed(t)

  override def map[T, T2](fa: Task[T])(f: (T) => T2): Task[T2] = fa.map(f)

  override def flatMap[T, T2](fa: Task[T])(f: (T) => Task[T2]): Task[T2] =
    fa.flatMap(f)

  override def error[T](t: Throwable): Task[T] = Task.fail(t)

  override protected def handleWrappedError[T](rt: Task[T])(h: PartialFunction[Throwable, Task[T]]): Task[T] =
    rt.catchSome(h)

  override def eval[T](t: => T): Task[T] = Task.effect(t)

  override def suspend[T](t: => Task[T]): Task[T] = Task.effectSuspend(t)

  override def fromTry[T](t: Try[T]): Task[T] = Task.fromTry(t)

  override def async[T](register: (Either[Throwable, T] => Unit) => Canceler): Task[T] =
    Task.effectAsyncInterrupt { cb =>
      val canceler = register {
        case Left(t) => cb(Task.fail(t))
        case Right(t) => cb(Task.succeed(t))
      }

      Left(UIO(canceler.cancel()))
    }

  override def ensure[T](f: Task[T], e: => Task[Unit]): Task[T] = f.ensuring(e.ignore)
}