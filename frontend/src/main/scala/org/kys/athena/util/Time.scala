package org.kys.athena.util

object Time {
  // Renders interval of milliseconds to a string of format HH:mm:ss
  def renderMsInterval(intervalMs: Long): String = {
    val sign    = if (intervalMs > 0) "" else "-"
    val diff    = Math.abs(intervalMs)
    val h       = diff / (3600 * 1000)
    val m       = (diff - h * 3600 * 1000) / (60 * 1000)
    val s       = (diff - (h * (3600 * 1000) + m * (60 * 1000))) / 1000
    val hstr    = if (h > 0) s"$h:" else ""
    s"$sign$hstr${if (m < 10) s"0${m}" else m.toString}:${if (s < 10) s"0${s}" else s.toString}"
  }
}
