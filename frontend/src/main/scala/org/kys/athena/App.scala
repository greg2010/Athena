package org.kys.athena

import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import io.circe.parser._
import io.circe.syntax._
import org.kys.athena.components.{AppBar, Footer}
import org.kys.athena.pages._
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.views.{CurrentGameView, LandingView}
import org.scalajs.dom
import urldsl.errors.DummyError
import urldsl.vocabulary.{FromString, Printer}


object App {

  private val routes: List[Route[_ <: Page, _]] = List(
    Route.static(LandingPage, root / endOfSegments),
    Route[CurrentGamePage, (Platform, String)](
      encode = p => (p.realm, p.name),
      decode = a => CurrentGamePage(a._1, a._2),
      pattern = {
        implicit val fs = new FromString[Platform, DummyError] {
          override def fromString(str: String): Either[DummyError, Platform] =
            Platform.withNameEither(str).left.map(_ => DummyError.dummyError)
        }
        implicit val pr = new Printer[Platform] {
          override def print(t: Platform): String = t.entryName
        }
        root / segment[Platform] / segment[String] / endOfSegments
      }
    )
  )
  private val router = new Router[Page](
    initialUrl = dom.document.location.href,
    origin = dom.document.location.origin.get,
    routes = routes,
    owner = unsafeWindowOwner, // this router will live as long as the window
    $popStateEvent = windowEvents.onPopState,
    getPageTitle = _.title, // mock page title (displayed in the browser tab next to favicon)
    serializePage = page => page.asJson.noSpaces, // serialize page data for storage in History API log
    deserializePage = pageStr => decode[Page](pageStr).fold(e => ErrorPage(e.getMessage), identity) // deserialize the above
  )


  private val splitter: SplitRender[Page, HtmlElement] =
    SplitRender[Page, HtmlElement](router.$currentPage)
    .collectStatic(LandingPage) {
      LandingView.render()
    }.collectStatic(PageNotFound) {
      LandingView.render()
    }.collectStatic(PlayerNotFound) {
      LandingView.render()
    }.collect[CurrentGamePage] { page =>
      CurrentGameView.render(page)
    }

  def render(): HtmlElement = {
    div(cls := "root h-screen flex flex-col items-center",
        div(cls := "bg-gradient-to-b h-full w-full from-white to-gray-400 fixed", zIndex := "-10"),
        AppBar.render(router.$currentPage.map {
          case LandingPage => false
          case _ => true
        }),
        div(cls := "container justify-center flex flex-grow", child <-- splitter.$view),
        Footer.render())
  }

  def pushState(page: Page): Unit = {
    router.pushState(page)
  }

  def replaceState(page: Page): Unit = {
    router.replaceState(page)
  }

  def relativeUrlForPage(page: Page): String = {
    router.relativeUrlForPage(page)
  }
}
