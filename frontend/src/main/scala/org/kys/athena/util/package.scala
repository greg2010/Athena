package org.kys.athena

import org.kys.athena.http.errors.BackendApiError


package object util {

  sealed trait DataState[+T] { self =>
    def fold[T2](onLoading: => T2, onFailed: => T2, onReady: T => T2): T2 = {
      self match {
        case Loading => onLoading
        case Failed(_) => onFailed
        case Ready(v) => onReady(v)
      }
    }

    def map[T2](f: T => T2): DataState[T2] = {
      self match {
        case Ready(v) => Ready(f(v))
        case Loading => Loading
        case Failed(e) => Failed(e)
      }
    }

    def flatMap[T2](f: T => DataState[T2]): DataState[T2] = {
      self match {
        case Ready(v) => f(v)
        case Loading => Loading
        case e: Failed => e
      }
    }

    def zip[T2](other: DataState[T2]): DataState[(T, T2)] = {
      (self, other) match {
        case (Ready(v), Ready(v2)) => Ready((v, v2))
        case (e: Failed, _) => e
        case (_, e: Failed) => e
        case (Loading, _) => Loading
        case (_, Loading) => Loading
      }
    }

    def toOption: Option[T] = {
      self match {
        case Ready(v) => Some(v)
        case _ => None
      }
    }
  }

  sealed trait Infallible[+T] extends DataState[T] { self =>
    override def map[T2](f: T => T2): Infallible[T2] = {
      self match {
        case Loading => Loading
        case Ready(data) => Ready(f(data))
      }
    }

    def flatMap[T2](f: T => Infallible[T2]): Infallible[T2] = {
      self match {
        case Loading => Loading
        case Ready(data) => f(data)
      }
    }

    def zip[T2](other: Infallible[T2]): Infallible[(T, T2)] = {
      (self, other) match {
        case (Loading, _) => Loading
        case (_, Loading) => Loading
        case (Ready(v), Ready(v2)) => Ready((v, v2))
      }
    }
  }

  case object Loading extends Infallible[Nothing] {
  }
  case class Ready[T](data: T) extends Infallible[T]

  case class Failed(err: BackendApiError) extends DataState[Nothing]
}
