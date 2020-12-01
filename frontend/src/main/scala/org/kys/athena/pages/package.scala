package org.kys.athena


import io.circe.generic.semiauto._
import org.kys.athena.riot.api.dto.common.Platform


package object pages {
  sealed trait Page {
    val title: String
  }

  final case object LandingPage extends Page {
    override val title = "Athena"
  }
  final case object PageNotFound extends Page {
    override val title: String = "Athena - Page not Found"
  }
  final case object PlayerNotFound extends Page {
    override val title: String = "Athena - Player not Found"
  }
  final case class ErrorPage(msg: String) extends Page {
    override val title: String = "Athena - Error"
  }
  final case class CurrentGamePage(realm: Platform, name: String) extends Page {
    override val title: String = s"Athena - $realm/$name"
  }

  implicit val codecPage = deriveCodec[Page]
}
