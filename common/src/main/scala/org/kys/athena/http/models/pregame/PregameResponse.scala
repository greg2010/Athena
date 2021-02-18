package org.kys.athena.http.models.pregame

import java.util.UUID


final case class PregameResponse(summoners: Set[PregameSummoner], groupUuid: Option[UUID] = None)

