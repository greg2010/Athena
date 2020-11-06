package org.kys.athena.http.models

final case class RunesSelected(primaryPathId: Long, secondaryPathId: Long, keystone: Long, runeIds: List[Long])
