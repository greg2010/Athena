package org.kys.athena.util

final case class Config(riotApiKey: String,
                        riotApiIsProd: Boolean,
                        http: Http,
                        cacheRiotRequestsFor: Int,
                        cacheRiotRequestsMaxCount: Long)

final case class Http(host: String, port: Int, prefix: String)
