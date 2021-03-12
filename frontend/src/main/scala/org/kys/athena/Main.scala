package org.kys.athena

import com.raquo.laminar.api.L._
import org.kys.athena.datastructures.Config
import org.scalajs.dom.document


object Main {
  def main(args: Array[String]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      Config.ANALYTICS_SCRIPT_URL match {
        case "" => {
          scribe.info("No analytics tracker url provided, skipping tracking")
        }
        case url => {
          scribe.debug(s"Injecting analytics tracker with url=$url")
          val scr = document.createElement("script")
          scr.setAttribute("src", url)
          scr.setAttribute("async", "")
          scr.setAttribute("defer", "")
          scr.setAttribute("data-website-id", Config.ANALYTICS_WEBSITE_ID)
          document.head.appendChild(scr)
        }
      }

      val container = document.getElementById("root")
      render(container, App.render())
    }(unsafeWindowOwner)
  }
}