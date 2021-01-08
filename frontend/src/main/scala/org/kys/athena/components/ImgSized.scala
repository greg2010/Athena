package org.kys.athena.components

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.kys.athena.util.Imgproxy
import org.scalajs.dom.html

object ImgSized {
  def apply(url: String, imgWidth: Int, imgHeight: Option[Int]): ReactiveHtmlElement[html.Image] = {
    val resizedImgUrl = Imgproxy.resizeUrl(url, imgWidth, imgHeight.getOrElse(0))
    img(
      src := resizedImgUrl,
      width := s"${imgWidth}px",
      imgHeight match {
        case Some(v) => height := s"${v}px"
        case None => None
      }
    )
  }
}
