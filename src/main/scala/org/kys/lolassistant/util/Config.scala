package org.kys.lolassistant.util

case class Config(riotApiKey: String, http: Http)

case class Http(host: String, port: Int, prefix: String)