package org.kys.athena.components.common

import com.raquo.laminar.api.L._
import org.kys.athena.util.assets.AssetLoader

import scala.math._


object AlephEye {
  def apply(): HtmlElement = {
    //center of eye aleph256full.png (152,125)
    val referenceDiv = div(
      position := "relative",
      top := "44.5%",
      left := "56%",
      width := "0px",
      height := "0px")

    val eyeCoords = windowEvents.onMouseMove.map { ev =>
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

    div(
      width := "256px",
      height := "256px",
      cls := "ml-1",
      div(
        position := "absolute",
        height := "256px",
        div(
          img(
            src := AssetLoader.require("/images/aleph256full.png"),
            width := "256px",
            height := "256px"),
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
        backgroundColor := "#a6a6a6"),
      //reference point of eye
      referenceDiv)
  }
}
