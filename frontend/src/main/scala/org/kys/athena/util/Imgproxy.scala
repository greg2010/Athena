package org.kys.athena.util

import sttp.client3._
import java.util.Base64
import org.kys.athena.datastructures.Config

object Imgproxy {
  private val encoder: Base64.Encoder = Base64.getEncoder

    if (Config.IMGPROXY_BASE_URL == "") {
      scribe.warn("No ImgProxy base url detected. Assuming service is not available. " +
                  "Images will not be resized before load.")
    }

  def resizeUrl(url: String, width: Int, height: Int): String = {
    if (Config.IMGPROXY_BASE_URL == "") {
      url
    } else {
      val b64url     = new String(encoder.encode(url.getBytes))
      uri"${Config.IMGPROXY_BASE_URL}/fill/$width/$height/ce/0/$b64url".toString()
    }
  }
}
