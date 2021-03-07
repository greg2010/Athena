package org.kys.athena.util


import com.raquo.laminar.api.L._
import org.kys.athena.riot.api.dto.common.Platform
import io.circe.generic.auto._

object SearchHistoryManager {
  case class HistorySummoner(name: String, platform: Platform)

  private val lsKey = "searchCache"
  // Needs to be divisible by 4
  private val maxUserSearchesSaved = 20

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
  private val historyVar = Var[List[HistorySummoner]](getCache())

  // Register a global listener that will write changes to cache
  historyVar.signal.foreach { elem =>
    CacheManager.setSync(lsKey, elem) match {
      case Left(err) =>
      scribe.error("Error while writing search data", err)
      case Right(_) => ()
    }
  }(unsafeWindowOwner)

  // Public interface

  def saveSearch(currentUserSearch: HistorySummoner): Unit = {
    val current = historyVar.now()
    
    val newCacheData = if (current.contains(currentUserSearch)) {
      // Current search is already present, moving up
      current.filterNot(_ == currentUserSearch).prepended(currentUserSearch)
    } else {
      current.prepended(currentUserSearch)
    }

    historyVar.writer.onNext(newCacheData.take(maxUserSearchesSaved))
  }

  def removeSearch(localSearchData: HistorySummoner): Unit = {
    val current = historyVar.now()
    historyVar.writer.onNext(current.filterNot(_ == localSearchData))
  }

  val historySignal = historyVar.signal
}
