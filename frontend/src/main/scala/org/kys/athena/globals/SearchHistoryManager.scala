package org.kys.athena.util


import com.raquo.laminar.api.L._
import org.kys.athena.riot.api.dto.common.Platform
import io.circe.generic.auto._
import org.kys.athena.routes.OngoingRoute
import org.kys.athena.App

object SearchHistoryManager {
  case class HistorySummoner(name: String, platform: Platform, isStarred: Boolean, savedAt: Long)

  private val lsKey = "searchCache"
  // Needs to be divisible by 4
  private val maxUserSearchesSaved = 20

  private implicit val ordering: Ordering[HistorySummoner] =
      Ordering.by[HistorySummoner, Boolean](!_.isStarred).orElseBy(_.savedAt * -1)

  // Helper: load data from cache
  private def getCache(): List[HistorySummoner] = {
    CacheManager.getSync[List[HistorySummoner]](lsKey) match {
      case Left(err) =>
      scribe.error("Error while getting search history from cache", err)
      List()
      case Right(res) => res.fold(List[HistorySummoner]())(identity)
    }
  }

  // Reactive variable that will keep track of current history state
  private val historyVar = Var[List[HistorySummoner]](getCache().sorted)

  // Register a global listener that will write changes to cache
  historyVar.signal.foreach { elem =>
    CacheManager.setSync(lsKey, elem) match {
      case Left(err) =>
      scribe.error("Error while writing search data", err)
      case Right(_) => ()
    }
  }(unsafeWindowOwner)

  // Register a global listener that will save visited pages to history cache
  App.routerSignal.foreach {
    case o: OngoingRoute => saveSearch(o.name, o.realm)
    case _ => ()
  }(unsafeWindowOwner)

  // Public interface

  def saveSearch(name: String, platform: Platform): Unit = {
    val current = historyVar.now()

    val toSave = current.find(p => p.name == name && p.platform == platform) match {
      case Some(v) => v.copy(savedAt = System.currentTimeMillis())
      case None => HistorySummoner(name, platform, isStarred = false, System.currentTimeMillis())
    }

    val newCacheData = current.filterNot(p => p.name == name && p.platform == platform).prepended(toSave)

    historyVar.set(newCacheData.sorted.take(maxUserSearchesSaved))
  }

  def star(name: String, platform: Platform): Unit = {
    val current = historyVar.now()
    historyVar.set(
      current.map {
        case hs: HistorySummoner if hs.name == name && hs.platform == platform => hs.copy(isStarred = true)
        case o => o
      }
    )
  }

  def unstar(name: String, platform: Platform): Unit = {
    val current = historyVar.now()
    historyVar.set(
      current.map {
        case hs: HistorySummoner if hs.name == name && hs.platform == platform => hs.copy(isStarred = false)
        case o => o
      }
    )
  }

  def removeSearch(name: String, platform: Platform): Unit = {
    val current = historyVar.now()
    historyVar.set(current.filterNot(p => p.name == name && p.platform == platform))
  }

  val historySignal: Signal[List[HistorySummoner]] = historyVar.signal
}
