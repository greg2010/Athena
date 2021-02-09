package org.kys.athena.components

import com.raquo.laminar.api.L._
import org.kys.athena.riot.api.dto.common.Platform


object LandingPage {

  def render: HtmlElement = {
    div(cls := "flex flex-col items-center container-md flex-grow justify-center",
        img(src := "/blitzcrank_logo.png", width := "256px", height := "256px"),
        common.SearchBar("", Platform.NA,
                         cls := "border shadow-lg border-gray-500 rounded-lg bg-white w-11/12 lg:w-8/12 h-12"))
  }
}