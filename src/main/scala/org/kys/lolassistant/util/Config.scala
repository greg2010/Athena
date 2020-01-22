package org.kys.lolassistant.util

case class Config(riotApiKey: String, http: Http, cacheRiotRequestsFor: Int, cacheRiotRequestsMaxCount: Long)

case class Http(host: String, port: Int, prefix: String)