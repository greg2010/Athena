package org.kys.athena.util


import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("window._env_")
object Config extends js.Object {
  val DDRAGON_VERSION  : String = js.native
  val LOCALE           : String = js.native
  val DDRAGON_BASE_URL : String = js.native
  val IMGPROXY_BASE_URL: String = js.native
  val BACKEND_API_URL  : String = js.native
  val FRONTEND_URL     : String = js.native
  val LOGLEVEL         : String = js.native
}
