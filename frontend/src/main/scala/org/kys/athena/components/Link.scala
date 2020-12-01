package org.kys.athena.components

import com.raquo.laminar.api.L._
import org.kys.athena.App
import org.kys.athena.pages.Page

object Link {

  def apply[T](page: Page, mods: Modifier[HtmlElement]*): HtmlElement = {
    a(
      href := App.relativeUrlForPage(page),
      onClick.preventDefault --> { _ =>
        App.pushState(page)
      },
      mods
    )
  }

}
