package org.kys.athena.components

import com.raquo.laminar.api.L._
import org.kys.athena.pages.LandingPage


object AppBar {

  def render(showSearch: Signal[Boolean]): HtmlElement = {
    nav(cls := "shadow-lg bg-blue-900 w-full px-3 py-2 flex items-center justify-between h-14",
      Link(LandingPage, span(cls := "font-medium text-white", "Athena")),
      child <-- showSearch.map {
        case true => SearchBar.render("", "h-8", "text-md")
        case false => div()
      })
  }
}
