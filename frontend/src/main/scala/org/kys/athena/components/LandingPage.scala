package org.kys.athena.components

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L._
import org.kys.athena.components.common._
import org.kys.athena.components.common.HistoryMenu
import org.kys.athena.riot.api.dto.common.Platform
import org.scalajs.dom.MouseEvent
import scala.math._

object LandingPage {

  sealed trait EventFired
  case object FocusIn extends EventFired
  case object FocusOut extends EventFired

  def render(mouseES:EventStream[MouseEvent]): HtmlElement = {
    val referenceDiv = div(
      position := "relative",
      top := "44.5%",
      left := "56%",
      width := "0px",
      height := "0px")

    val eyeCoords = mouseES.map { ev =>
      val eyeRef = referenceDiv.ref.getBoundingClientRect()
      // calc theta from mouse coordinates
      val th     = atan2(ev.pageY - eyeRef.top, ev.pageX - eyeRef.left)
      // determine distance % mouse is from center and then multiply by polar distance coord "r"
      // (tan-1 (y/x) multiplied by max allowed circle draw
      val r      = sqrt(pow(1, 2) + pow(5, 2))
      // convert back to cartesian
      val x      = r * cos(th)
      val y      = r * sin(th)

      (x, y)
    }.toSignal((-5D,0D))
    val focusBus = new EventBus[EventFired]
    div(cls := "flex flex-col items-center container-md flex-grow justify-center",
      //center of eye aleph256full.png (152,125)


        div(
          width := "256px",
          height := "256px",
          cls := "ml-1",
          div(
            position := "absolute",
            height := "256px",
            div(img(src := "/images/aleph256full.png", width := "256px", height := "256px"),
                zIndex := 1)),

            div(
              position := "relative",
              // draw eye at coordinates
              top <-- eyeCoords.map(coords => s"${coords._2 + 44.5}%"),
              left <-- eyeCoords.map(coords => s"${coords._1 + 56}%"),
              zIndex := 2,
              cls := "rounded-full flex items-center justify-center",
              width := "20px",
              height := "20px",
              backgroundColor := "#a6a6a6",
              ),

          //reference point of eye
            referenceDiv
          ),
        span(fontFamily := "heorotregular",
             fontSize := "6rem",
             color := "#780522",
             lineHeight := "0.5",
             cls := "pt-2 pb-3", "Athena"),
        span(cls := "font-bold pb-4", "A solo queue companion"),
        div(
          cls := "flex flex-col items-center justify-center " +
                 "border shadow-lg border-gray-500 rounded-lg bg-white w-11/12 lg:w-8/12 " +
                 "p-1 divide-y divide-gray-500",
          onFocus.preventDefault.useCapture.mapTo(FocusIn) --> focusBus.writer,
          onBlur.preventDefault.useCapture.mapTo(FocusOut) --> focusBus.writer,
          SearchBar("", Platform.NA, cls := "w-full pb-1"),
          div(
            cls := "w-full",
            HistoryMenu(Some("p-1 font-sans"),
                       cls := "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 justify-center pt-1 gap-1"),
            cls <-- focusBus.events.delay(100).toSignal(FocusOut)
                            .combineWith(SearchHistoryManager.historySignal.map(_.length)).map {
              case (FocusOut, _) => "hidden"
              case (_, 0) => "hidden"
              case _ => ""
           }
          )))
  }
}


