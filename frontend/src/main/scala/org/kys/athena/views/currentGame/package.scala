package org.kys.athena.views

import org.kys.athena.http.errors.BackendApiError


package object currentGame {


  sealed trait DataState[T] {
    def fold[T2](onLoading: => T2, onFailed: => T2, onReady: T => T2): T2

    def map[T2](f: T => T2): DataState[T2]

    def flatMap[T2](f: T => DataState[T2]): DataState[T2]

    def zip[T2](other: DataState[T2]): DataState[(T, T2)]

    def toOption: Option[T]
  }

  sealed trait Infallible[T] extends DataState[T] {
    def map[T2](f: T => T2): Infallible[T2]

    def flatMap[T2](f: T => Infallible[T2]): Infallible[T2]

    def zip[T2](other: Infallible[T2]): Infallible[(T, T2)]
  }

  case class Loading[T]() extends Infallible[T] {
    def fold[T2](onLoading: => T2, onFailed: => T2, onReady: T => T2): T2 = onLoading

    def map[T2](f: T => T2): Infallible[T2] = Loading[T2]()

    def flatMap[T2](f: T => DataState[T2]): DataState[T2] = Loading[T2]()

    def flatMap[T2](f: T => Infallible[T2]): Infallible[T2] = Loading[T2]()

    def zip[T2](other: DataState[T2]): DataState[(T, T2)] = Loading[(T, T2)]()

    def zip[T2](other: Infallible[T2]): Infallible[(T, T2)] = Loading[(T, T2)]()

    def toOption: Option[T] = None
  }
  case class Ready[T](data: T) extends Infallible[T] {
    def fold[T2](onLoading: => T2, onFailed: => T2, onReady: T => T2): T2 = onReady(data)

    def map[T2](f: T => T2): Infallible[T2] = Ready[T2](f(data))

    def flatMap[T2](f: T => DataState[T2]): DataState[T2] = f(data)

    def flatMap[T2](f: T => Infallible[T2]): Infallible[T2] = f(data)

    def zip[T2](other: DataState[T2]): DataState[(T, T2)] = other.flatMap(ov => Ready(data, ov))

    def zip[T2](other: Infallible[T2]): Infallible[(T, T2)] = other.flatMap(ov => Ready(data, ov))

    def toOption: Option[T] = Some(data)
  }
  case class Failed[T](err: BackendApiError) extends DataState[T] {
    def fold[T2](onLoading: => T2, onFailed: => T2, onReady: T => T2): T2 = onFailed

    def map[T2](f: T => T2): DataState[T2] = Failed[T2](err)

    def flatMap[T2](f: T => DataState[T2]): DataState[T2] = Failed[T2](err)

    def zip[T2](other: DataState[T2]): DataState[(T, T2)] = Failed[(T, T2)](err)

    def zip[T2](other: Infallible[T2]): Infallible[(T, T2)] = Loading[(T, T2)]()

    def toOption: Option[T] = None
  }
}
