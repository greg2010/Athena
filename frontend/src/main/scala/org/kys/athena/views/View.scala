package org.kys.athena.views

import com.raquo.laminar.api.L._
import org.kys.athena.pages.Page

trait View[P <: Page] {

  def render(p: P): HtmlElement
}
