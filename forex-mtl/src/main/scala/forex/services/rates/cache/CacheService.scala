package forex.services.rates.cache

import cats.effect.Sync
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.typesafe.scalalogging.LazyLogging
import forex.domain.Rate.Pair
import forex.domain.{Price, Rate}

import java.util.concurrent.TimeUnit

/**
 * Service for caching Forex rates and resource server statuses.
 *
 * @param cacheConfig Configuration for cache settings.
 */
class CacheService[F[_]: Sync](cacheConfig: CacheConfig = CacheConfig()) extends LazyLogging {

  // Cache for storing Forex rates, keyed by currency pair
  private val ratesCache: Cache[Pair, RateCacheValue] = Caffeine.newBuilder()
    .expireAfterWrite(cacheConfig.RatesCache.expireAfterWriteMinutes, TimeUnit.MINUTES)
    .maximumSize(cacheConfig.RatesCache.maximumSize)
    .build[Pair, RateCacheValue]()

  // Cache for storing resource server statuses, keyed by server name
  private val resourceServerCache: Cache[String, Boolean] = Caffeine.newBuilder()
    .expireAfterWrite(cacheConfig.ResourceServerCache.expireAfterWriteDays, TimeUnit.DAYS)
    .maximumSize(cacheConfig.ResourceServerCache.maximumSize)
    .build[String, Boolean]()

  /**
   * Stores a list of Forex rates in the cache.
   *
   * @param rates The list of Forex rates to be cached.
   * @return Effect that represents storing rates in the cache.
   */
  def storeInRatesCache(rates: List[Rate]): F[Unit] = {
    Sync[F].delay {
      rates.foreach { rate =>
        val priceInfo = RateCacheValue(Price(rate.price.value), rate.timestamp)
        ratesCache.put(rate.pair, priceInfo)
      }
      logger.info("Stored {} pairs of rates in cache.", rates.size)
    }
  }

  /**
   * Retrieves a Forex rate from the cache based on the currency pair.
   *
   * @param pair The currency pair to look up.
   * @return An effect wrapping an optional Rate if the pair is found in the cache.
   */
  def getRateFromCache(pair: Pair): F[Option[Rate]] = {
    Sync[F].delay {
      val cachedRate = Option(ratesCache.getIfPresent(pair))
      cachedRate match {
        case Some(value) =>
          logger.info("Cache hit for pair: {} -> {}", pair.from, pair.to)
          Some(Rate(pair, value.price, value.timestamp))
        case None =>
          logger.warn("Cache miss for pair: {} -> {}", pair.from, pair.to)
          None
      }
    }
  }

  /**
   * Stores the status of a resource server in the cache.
   *
   * @param serverName The name of the resource server.
   * @return Effect that represents storing the status in the cache.
   */
  def storeResourceServerStatus(serverName: String): F[Unit] = {
    Sync[F].delay {
      resourceServerCache.put(serverName, true)
      logger.info("Stored resource server status for: {}", serverName)
    }
  }

  /**
   * Retrieves the status of a resource server from the cache.
   *
   * @param serverName The name of the resource server.
   * @return An effect wrapping an optional Boolean indicating the status of the server.
   */
  def getResourceServerStatus(serverName: String): F[Option[Boolean]] = {
    Sync[F].delay {
      Option(resourceServerCache.getIfPresent(serverName))
    }
  }

  /**
   * Evicts a specific entry from the resource server cache by the given key.
   *
   * @param key The key of the entry to be evicted.
   * @return Effect that represents the eviction operation.
   */
  def evictResourceServerCache(key: String): F[Unit] = {
    Sync[F].delay {
      resourceServerCache.invalidate(key)
      logger.info(s"Evicted cache entry for key: $key")
    }
  }
}

object CacheService {
  def apply[F[_]: Sync](): CacheService[F] = new CacheService[F]()
  def apply[F[_]: Sync](config: CacheConfig): CacheService[F] = new CacheService[F](config)
}