package org.kys.athena

import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import io.circe.parser._
import io.circe.syntax._
import org.kys.athena.routes._
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.util.CSSUtil
import org.kys.athena.components.LandingPage
import org.kys.athena.components.common.{AppBar, Footer}
import org.kys.athena.components.ongoing.OngoingPage
import org.scalajs.dom
import urldsl.errors.DummyError
import urldsl.vocabulary.{FromString, Printer}


object App {

  private val routes: List[Route[_ <: PageRoute, _]] = List(
    Route.static(LandingRoute, root / endOfSegments),
    Route[OngoingRoute, (Platform, String)](
      encode = p => (p.realm, p.name),
      decode = a => OngoingRoute(a._1, a._2),
      pattern = {
        implicit val fs = new FromString[Platform, DummyError] {
          override def fromString(str: String): Either[DummyError, Platform] = {
            Platform.withNameEither(str).left.map(_ => DummyError.dummyError)
          }
        }
        implicit val pr = new Printer[Platform] {
          override def print(t: Platform): String = t.entryName
        }
        root / segment[Platform] / segment[String] / endOfSegments
      })
    )

  private val router = new Router[PageRoute](
    initialUrl = dom.document.location.href,
    origin = dom.document.location.origin.get,
    routes = routes,
    owner = unsafeWindowOwner, // this router will live as long as the window
    $popStateEvent = windowEvents.onPopState,
    getPageTitle = _.title, // mock page title (displayed in the browser tab next to favicon)
    serializePage = page => page.asJson.noSpaces, // serialize page data for storage in History API log
    deserializePage = pageStr => {
      decode[PageRoute](pageStr).fold(e => ErrorRoute(e.getMessage), identity)
    } // deserialize the above
    )

  private val hideSearchBar = Var(false)


  private val splitter: SplitRender[PageRoute, HtmlElement] =
    SplitRender[PageRoute, HtmlElement](router.$currentPage)
      .collectStatic(LandingRoute) {
        LandingPage.render
      }.collectStatic(RouteNotFound) {
      LandingPage.render
    }.collect[OngoingRoute] { page =>
      OngoingPage.render(page, hideSearchBar.writer)
    }

  def render(): HtmlElement = {
    div(cls := "root h-screen flex flex-col items-center",
        div(cls := "h-full w-full fixed",
            backgroundColor := CSSUtil.paletteBackground,
            zIndex := "-10"),
        AppBar(router.$currentPage.combineWith(hideSearchBar.signal).map {
          case (LandingRoute, _) => false
          case (_: OngoingRoute, sig) => !sig
          case _ => true
        }, router.$currentPage.map {
          case LandingRoute => false
          case _ => true
        }),
        div(cls := "container justify-center items-center flex flex-grow", child <-- splitter.$view),
        Footer())
  }

  def pushState(page: PageRoute): Unit = {
    router.pushState(page)
  }

  def replaceState(page: PageRoute): Unit = {
    router.replaceState(page)
  }

  def relativeUrlForPage(page: PageRoute): String = {
    router.relativeUrlForPage(page)
  }
}
