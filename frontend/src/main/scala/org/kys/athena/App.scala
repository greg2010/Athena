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
import org.kys.athena.components.notfound.NotFoundPage
import org.kys.athena.components.ongoing.OngoingPage
import org.kys.athena.components.pregame.PregamePage
import org.scalajs.dom
import org.scalajs.dom.document
import urldsl.errors.DummyError
import urldsl.vocabulary.{FromString, Printer, UrlMatching}


object App {
  private implicit val platformDecoder: FromString[Platform, DummyError] = new FromString[Platform, DummyError] {
    override def fromString(str: String): Either[DummyError, Platform] = {
      Platform.withNameEither(str).left.map(_ => DummyError.dummyError)
    }
  }

  private implicit val platformPrinter: Printer[Platform] = new Printer[Platform] {
    override def print(t: Platform): String = t.entryName
  }

  private implicit val listDecoder: FromString[List[String], DummyError] = new FromString[List[String], DummyError] {
    override def fromString(str: String): Either[DummyError, List[String]] = Right(str.split(',').toList)
  }

  private implicit val listPrinter: Printer[List[String]] = new Printer[List[String]] {
    override def print(t: List[String]) = t.mkString(",")
  }

  private val routes: List[Route[_ <: PageRoute, _]] = List(
    Route.static(LandingRoute, root / endOfSegments),
    Route.withQuery[PregameRoute, Platform, List[String]](
      encode = p => UrlMatching(p.realm, p.names),
      decode = a => PregameRoute(a.path, a.params),
      pattern = (root / segment[Platform] / "pregame" / endOfSegments) ?
                (param[List[String]]("summoners"))),
    Route[OngoingRoute, (Platform, String)](
      encode = p => (p.realm, p.name),
      decode = a => OngoingRoute(a._1, a._2),
      pattern = root / segment[Platform] / segment[String] / endOfSegments),
    // This must be last (catch-all to prevent the router from crashing)
    Route[RouteNotFound, List[String]](
      encode = p => p.restOfSegments,
      decode = a => RouteNotFound(a),
      pattern = root / remainingSegments))

  private val router = new Router[PageRoute](
    initialUrl = dom.document.location.href,
    origin = dom.document.location.origin.get,
    routes = routes,
    owner = unsafeWindowOwner, // this router will live as long as the window
    $popStateEvent = windowEvents.onPopState,
    getPageTitle = _.title, // Currently ignored by most browsers (per MDN api spec)
    serializePage = page => page.asJson.noSpaces, // serialize page data for storage in History API log
    deserializePage = pageStr => {
      decode[PageRoute](pageStr).fold(e => RouteNotFound(List("error")), identity)
    })

  // Currently title in pushState is ignored by most (all?) browsers. To fix, dispatch a custom event
  // to update the title on route events
  router.$currentPage.foreach { page =>
    document.title = page.title
  }(unsafeWindowOwner)

  private val hideSearchBar = Var(false)

  private val splitter: SplitRender[PageRoute, HtmlElement] =
    SplitRender[PageRoute, HtmlElement](router.$currentPage).collectStatic(LandingRoute) {
      LandingPage.render
    }.collect[OngoingRoute] { page =>
      OngoingPage.render(page, hideSearchBar.writer)
    }.collect[PregameRoute] { page =>
      PregamePage.render(page)
    }.collect[RouteNotFound] { page =>
      NotFoundPage.render(page.restOfSegments)
    }


  def render(): HtmlElement = {
    div(cls := "root h-screen flex flex-col items-center",
        div(cls := "h-full w-full fixed",
            backgroundColor := CSSUtil.paletteBackground,
            zIndex := "-10"),
        AppBar(hideSearchBar.signal),
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

  val routerSignal = router.$currentPage
}
