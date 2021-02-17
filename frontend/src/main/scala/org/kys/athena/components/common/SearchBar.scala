package org.kys.athena.components.common


import com.raquo.laminar.api.L._
import org.kys.athena.App
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.routes.OngoingRoute
import org.scalajs.dom
import org.scalajs.dom.Event
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}


object SearchBar {
  case class LocalSearchData(name:String, platform:Platform)
  def saveToStorage(currentUserSearch:LocalSearchData):Unit = {
    val listFromStorage = readFromStorage
    val listCompareFalse = !listFromStorage.contains(currentUserSearch)
    val newList = if (listCompareFalse){
      listFromStorage :+ currentUserSearch
    } else {
      listFromStorage.lastOption match {
        case Some(last) =>
          if(currentUserSearch == last){
            listFromStorage
          }
          else{
            val listFromStorageFiltered = listFromStorage.filterNot(search => search == currentUserSearch)
            //move search to last pos

            //step3
            listFromStorageFiltered :+ currentUserSearch
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
  def removeFromStorage(localSearchData: LocalSearchData):Unit ={
    val listFromStorage = readFromStorage
    val userSearchDelete = localSearchData
    val newList = listFromStorage.filterNot(search => search == userSearchDelete)
    val newListAsString = newList.asJson.noSpaces
    dom.window.localStorage.setItem(key="localStorageCache", newListAsString)
    scribe.warn(newListAsString)

  }
  def apply(initialSummoner: String, initialPlatform: Platform, mods: Modifier[HtmlElement]*): HtmlElement = {

    val summoner    : Var[String]     = Var[String](initialSummoner)
    val platform    : Var[Platform]   = Var[Platform](initialPlatform)
    val formObserver: Observer[Event] =
      Observer[dom.Event](onNext = _ => {
        (platform.now(), summoner.now()) match {
          case (_, "") => ()
          case (p, s) => saveToStorage(LocalSearchData(s,p))
            App.pushState(OngoingRoute(p, s))
        }
      })
    div(
      cls:= "flex flex-col",
      form(cls := s"px-3 py-1 flex items-center",
           input(placeholder := "Enter a summoner name",
                 cls := s"flex-grow min-w-0 focus:outline-none appearance-none",
                 value <-- summoner.signal,
                 inContext(thisNode => onChange.mapTo(thisNode.ref.value) --> summoner)),
           DropdownMenu[Platform](platform.signal.map(_.toString),
                                  Platform.values.toList,
                                  platform.writer,
                                  Some("focus:outline-none text-md"),
                                  Some(s"border shadow-lg border-gray-500 p-1 rounded-sm bg-white"),
                                  Some("focus:outline-none text-md"),
                                  cls := s"px-1 focus:outline-none appearance-none"),
           button(`type` := "submit", img(src := "/icons/search.svg", width := "24px", height := "auto")),
           onSubmit.preventDefault --> formObserver),
      div(readFromStorage.map(lsd => a(href := s"/${lsd.platform.entryName}/${lsd.name}",
                                       div(cls:= "flex flex-col",s"${lsd.platform} ",s"${lsd.name} ")))),
      mods
    )
  }
}
