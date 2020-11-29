package org.kys.athena.http.models.current

final case class RunesSelected(primaryPathId: Long, secondaryPathId: Long, keystone: Long, runeIds: List[Long])
