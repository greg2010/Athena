package org.kys.athena.components.notfound

import com.raquo.laminar.api.L._
import org.kys.athena.components.common.{AlephEye, Link}
import org.kys.athena.routes.LandingRoute
import org.kys.athena.util.CSSUtil.{paletteContainer, paperCls}


object NotFoundPage {
  def render(segments: List[String]): HtmlElement = {
    div(
      cls := s"flex flex-col items-center justify-center p-4 lg:p-8 divide-y divide-gray-500 $paperCls",
      backgroundColor := paletteContainer,
      AlephEye(),
      div(
        cls := "flex flex-col justify-center items-center",
        span(
          cls := "text-xl my-2",
          s"Oops! Page /${segments.mkString("/")} is not found. Make sure your URL is correct."
          ),
        Link(
          LandingRoute,
          button(
            cls := "px-4 py-2 border border-gray-500 rounded-lg mt-2",
            "Go home"))))
  }
}
