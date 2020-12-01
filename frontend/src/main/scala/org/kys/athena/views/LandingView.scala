package org.kys.athena.views

import com.raquo.laminar.api.L._
import org.kys.athena.components.SearchBar
import org.kys.athena.pages.LandingPage

object LandingView extends View[LandingPage.type] {

  override def render(p: LandingPage.type = LandingPage): HtmlElement = {
    div(cls := "flex flex-col items-center container-md flex-grow justify-center",
        img(src := "/blitzcrank_logo.png", width := "256px", height := "256px"),
        SearchBar.render("md:w-11/12 sm:w-11/12 lg:w-8/12"))
  }
}
