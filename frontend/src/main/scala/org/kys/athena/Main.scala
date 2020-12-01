package org.kys.athena

import com.raquo.laminar.api.L._
import org.scalajs.dom.document


object Main {
  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      val container = document.getElementById("root")
      render(container, App.render())
    }(unsafeWindowOwner)
  }
}