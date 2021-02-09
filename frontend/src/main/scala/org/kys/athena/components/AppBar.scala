package org.kys.athena.components

import com.raquo.laminar.api.L._
import org.kys.athena.pages.LandingPage
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.util.{CSSUtil, Config}


object AppBar {

  def apply(showSearch: Signal[Boolean]): HtmlElement = {
    nav(cls := "shadow-lg w-full px-3 py-2 flex items-center justify-between h-14",
        backgroundColor := CSSUtil.paletteHeader,
        Link(LandingPage, span(cls := "font-medium text-white", "Athena")),
        div(
          cls := "flex flex-row h-full",
          a(cls := "mx-2", href := "https://github.com/greg2010/Athena", target := "_blank",
            ImgSized(s"${Config.FRONTEND_URL}/gh_logo.png", 40, Some(40))),
          child <-- showSearch.map {
            case true => SearchBar("", Platform.NA, cls := "border shadow-lg rounded-lg bg-white")
            case false => div()
          })
        )
  }
}
