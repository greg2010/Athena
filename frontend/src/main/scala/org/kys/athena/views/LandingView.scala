package org.kys.athena.views

import com.raquo.laminar.api.L._
import org.kys.athena.components.SearchBar
import org.kys.athena.pages.LandingPage
import org.kys.athena.riot.api.dto.common.Platform

object LandingView {

  def render(p: LandingPage.type = LandingPage): HtmlElement = {
    div(cls := "flex flex-col items-center container-md flex-grow justify-center",
        img(src := "/blitzcrank_logo.png", width := "256px", height := "256px"),
        SearchBar("", Platform.NA,
                  cls := "border shadow-lg border-gray-500 rounded-lg bg-white w-11/12 lg:w-8/12 h-12"))
  }
}
