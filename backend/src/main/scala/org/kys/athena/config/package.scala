package org.kys.athena

import org.kys.athena.config.RiotRateLimits


package object config {
  final case class Config(riotApiKey: String,
                          riotApiIsProd: Boolean,
                          http: Http,
                          cacheRiotRequestsFor: Int,
                          cacheRiotRequestsMaxCount: Long,
                          logLevel: String) {
    val rateLimitList = if (riotApiIsProd) RiotRateLimits.prodRateLimit
                        else RiotRateLimits.devRateLimit
  }

  final case class Http(host: String, port: Int, prefix: String)
}
