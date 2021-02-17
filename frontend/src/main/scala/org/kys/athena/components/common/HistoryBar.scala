package org.kys.athena.components.common

import com.raquo.laminar.api.L._
import org.kys.athena.riot.api.dto.common.Platform
import org.scalajs.dom
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.kys.athena.routes.OngoingRoute

object HistoryBar {
  case class LocalSearchData(name:String, platform:Platform)
  def saveToStorage(currentUserSearch:LocalSearchData):Unit = {
    val listFromStorage = readFromStorage
    val listCompareFalse = !listFromStorage.contains(currentUserSearch)
    val newList = if (listCompareFalse){
      listFromStorage.prepended(currentUserSearch)
    } else {
      listFromStorage.lastOption match {
        case Some(last) =>
          if(currentUserSearch == last){
            listFromStorage
          } else {
            val listFromStorageFiltered = listFromStorage.filterNot(search => search == currentUserSearch)
            listFromStorageFiltered.prepended(currentUserSearch)
          }
        case None => List(currentUserSearch)
      }
    }
    val newListAsString = newList.asJson.noSpaces
    dom.window.localStorage.setItem(key="localStorageCache", newListAsString)
    scribe.warn(newListAsString)
  }

  def readFromStorage:List[LocalSearchData] = {
    val rawCache = dom.window.localStorage.getItem("localStorageCache")
    val decoded = decode[List[LocalSearchData]](rawCache)
    decoded match {
      case Left(_) => List()
      case Right(res) => res
    }
  }

  def removeFromStorage(localSearchData: LocalSearchData): Unit ={
    val listFromStorage = readFromStorage
    val userSearchDelete = localSearchData
    val newList = listFromStorage.filterNot(search => search == userSearchDelete)
    val newListAsString = newList.asJson.noSpaces
    dom.window.localStorage.setItem(key="localStorageCache", newListAsString)
    scribe.warn(newListAsString)
  }

  def apply(dropdownElemCls: Option[String], mods: Modifier[HtmlElement]*): HtmlElement = {
    div(
      readFromStorage.map { lsd =>
        Link(OngoingRoute(lsd.platform, lsd.name),
          span(cls := dropdownElemCls.fold("")(identity), s"${lsd.platform}/${lsd.name}"))
      }, mods)
  }
}
