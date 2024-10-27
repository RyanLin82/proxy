package forex.services.rates.cache

import com.typesafe.config.{Config, ConfigFactory}

class CacheConfig {
  private val config: Config = ConfigFactory.load().getConfig("cache-service")

  object RatesCache {
    val expireAfterWriteMilliseconds: Long = config.getConfig("rates-cache").getLong("expire-after-write-milliseconds")
    val maximumSize: Long = config.getConfig("rates-cache").getLong("maximum-size")
  }
}

object CacheConfig {
  def apply(): CacheConfig = new CacheConfig()
}
