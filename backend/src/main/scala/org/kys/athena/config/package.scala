package org.kys.athena



package object config {
  final case class Config(riotApiKey: String,
                          http: Http,
                          cacheRiotRequestsFor: Int,
                          cacheRiotRequestsMaxCount: Long,
                          logLevel: String) {
  }

  final case class Http(host: String, port: Int, prefix: String)
}
