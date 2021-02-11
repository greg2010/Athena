package org.kys.athena.components.common

import com.raquo.laminar.api.L._
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.routes.LandingRoute
import org.kys.athena.util.{CSSUtil, Config}


object AppBar {

  def apply(showSearch: Signal[Boolean], showTitleUrl: Signal[Boolean]): HtmlElement = {
    nav(cls := "shadow-lg w-full px-3 py-2 flex items-center justify-between h-14",
        backgroundColor := CSSUtil.paletteHeader,
        child <-- showTitleUrl.map {
          case true =>
            Link(LandingRoute,
                 span(cls := "font-medium text-white tracking-wider", fontFamily := "heorotregular", "Athena"))
          case false => div()
        },
        div(
          cls := "flex flex-row h-full",
          a(cls := "mx-2", href := "https://github.com/greg2010/Athena", target := "_blank",
            ImgSized(s"${Config.FRONTEND_URL}/images/gh_logo.png", 40, Some(40))),
          child <-- showSearch.map {
            case true => SearchBar("", Platform.NA, cls := "border shadow-lg rounded-lg bg-white")
            case false => div()
          })
        )
  }
}
