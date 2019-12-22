package org.red.lolassistant.util

import java.time.Instant
import java.util
import java.util.HashMap

import com.typesafe.scalalogging.LazyLogging
import net.rithms.riot.api.ApiMethod
import net.rithms.riot.api.request.Request

import scala.jdk.CollectionConverters._
import net.rithms.riot.api.request.ratelimit.RateLimitHandler
import net.rithms.riot.constant.Platform

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * [[AtomicRateLimitHandler]] provides request based rate limiting according to Riot's guidelines
 * It is backed by [[collection.concurrent.TrieMap]] and relies on synchronization for writes and `readOnlySnapshot`
 * for reads.
 * Note that the implementation is not ideal and it WILL break if conditions are right - the number of worker threads is high,
 * and the ratelimit window and request counts are low
 */
class AtomicRateLimitHandler(timeError: Duration) extends RateLimitHandler with LazyLogging {

  private case class RateLimitKey(platform: Platform, window: Duration)
  private val limitList: collection.concurrent.TrieMap[RateLimitKey, Long] = collection.concurrent.TrieMap[RateLimitKey, Long]()
  private val platformRequests: collection.concurrent.TrieMap[Platform, List[java.time.Instant]] = collection.concurrent.TrieMap[Platform, List[java.time.Instant]]()
  private def longestLimit: Option[Duration] = limitList.view.map(_._1.window).maxOption
  private def updateMapSync[T,Y](map: collection.concurrent.TrieMap[T,Y], k: T, v: Y): Unit = synchronized {
    map.update(k,v)
  }
  private def addOneMapSync[T,Y](map: collection.concurrent.TrieMap[T,Y], k: T, v: Y): Unit = synchronized {
    map.addOne(k,v)
  }
  private def removeMapSync[T,Y](map: collection.concurrent.TrieMap[T,Y], k: T): Unit = synchronized {
    map.remove(k)
  }

  private def instantInThePast(duration: Duration): Instant = {
    java.time.Instant.ofEpochMilli(java.time.Instant.now().toEpochMilli - duration.toMillis)
  }

  private def getIntervalCountMapFromHeaderField(headerField: util.List[String]): List[(Int, Int)] = {
    headerField.asScala.toList.headOption match {
      case Some(header) =>
        header.split(',').flatMap { csh =>
          csh.split(':') match {
            case Array(k, v, _*) => List[(Int, Int)]((k.toInt, v.toInt))
            case _ => List[(Int, Int)]()
          }
        }.toList
      case None => List[(Int, Int)]()
    }
  }

  private def registerRequestForPlatform(platform: Platform): Unit = {
    platformRequests.view.find(_._1 == platform) match {
      case Some((p, rl)) =>
        val newRl =
          rl.filter(_.isAfter(instantInThePast(longestLimit.getOrElse(0.seconds)))) :+
          java.time.Instant.now()
        updateMapSync(platformRequests, p ,newRl)
      case None =>
        addOneMapSync(platformRequests, platform, List(java.time.Instant.now()))
    }
    //logger.debug(s"Registering request for platform=$platform rl=${platformRequests.find(_._1 == platform).map(_._2)}")
  }

  private def determineDeferTime(platform: Platform): Option[Duration] = {
    limitList.view.toMap.flatMap { limit =>
      platformRequests.view.find(_._1 == platform).flatMap {
        case (_, rl) if rl.count(_.isAfter(instantInThePast(limit._1.window))) < limit._2 =>
          logger.debug(s"Determined requestCount=${rl.count(_.isAfter(instantInThePast(limit._1.window)))} " +
            s"platform=${platform} " +
            s"window=${limit._1.window} " +
            s"limit=${limit._2}")
          None
        case (_, rl) =>
          rl.filter(_.isAfter(instantInThePast(limit._1.window))).minOption.map { oldestReq =>
              (oldestReq.toEpochMilli + limit._1.window.toMillis - java.time.Instant.now().toEpochMilli).millis
          }
      }
    }.maxOption.map(dt => math.max(dt.toMillis, 0).millis)
  }

  override def onRequestAboutToFire(request: Request): Unit = synchronized {
    val platform = request.getObject.getPlatform
    determineDeferTime(platform) match {
      case Some(deferTime) =>
        logger.warn(s"Determined request to platform ratelimited, deferring for $deferTime")
        Thread.sleep(deferTime.toMillis)
      case None =>
    }
    registerRequestForPlatform(platform)
  }

  override def onRequestDone(request: Request): Unit = synchronized {
    val limits = getIntervalCountMapFromHeaderField(request.getResponse.getHeaderFields.get("X-App-Rate-Limit"))
    val platform = request.getObject.getPlatform

    val storedKeys = limits.map { limit =>
      limitList.view.find(e => e._1.platform == platform && e._1.window == limit._2.seconds) match {
        case None =>
          val key = RateLimitKey(platform, limit._2.seconds)
          logger.debug(s"Adding new limiter platform=$platform limit=${limit._2.seconds}")
          addOneMapSync(limitList, key, (limit._1.seconds + timeError).toSeconds)
          key
        case Some((key, limit._1)) => key // Limit already in memory, noop
        case Some((key, oldLimit)) =>
          logger.warn(s"Limit changed for platform=$platform duration=${key.window} oldLimit=$oldLimit newLimit=${limit._1}")
          updateMapSync(limitList, key, limit._1.toLong)
          key
      }
    }
    // Delete old limits
    limitList.view.filterKeys(k => storedKeys.contains(k))
  }
}
