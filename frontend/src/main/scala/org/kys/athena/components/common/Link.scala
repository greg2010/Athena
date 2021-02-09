package org.kys.athena.components.common


import com.raquo.laminar.api.L._
import org.kys.athena.App
import org.kys.athena.routes.PageRoute


object Link {

  def apply[T](page: PageRoute, mods: Modifier[HtmlElement]*): HtmlElement = {
    a(
      href := App.relativeUrlForPage(page),
      onClick.preventDefault --> { _ =>
        App.pushState(page)
      }, mods)
  }

}
