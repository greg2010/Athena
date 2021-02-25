package org.kys.athena


import io.circe.generic.semiauto._
import org.kys.athena.riot.api.dto.common.Platform


package object routes {
  sealed trait PageRoute {
    val title: String
  }

  final case object LandingRoute extends PageRoute {
    override val title = "Athena - Landing Page"
  }
  final case class RouteNotFound(restOfSegments: List[String]) extends PageRoute {
    override val title: String = "Athena - Page not Found"
  }

  final case class ErrorRoute(msg: String) extends PageRoute {
    override val title: String = "Athena - Error"
  }
  final case class OngoingRoute(realm: Platform, name: String) extends PageRoute {
    override val title: String = s"Athena - Current game of $realm/$name"
  }

  final case class PregameRoute(realm: Platform, names: List[String]) extends PageRoute {
    override val title: String = "Athena - Pregame Lobby"
  }

  implicit val codecPage = deriveCodec[PageRoute]
}
