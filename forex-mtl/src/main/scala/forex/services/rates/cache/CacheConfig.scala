package forex.services.rates.cache

import com.typesafe.config.{Config, ConfigFactory}

class CacheConfig {
  private val config: Config = ConfigFactory.load().getConfig("cache-service")

  object RatesCache {
    val expireAfterWriteMinutes: Long = config.getConfig("rates-cache").getLong("expire-after-write-minutes")
    val maximumSize: Long = config.getConfig("rates-cache").getLong("maximum-size")
  }
}

object CacheConfig {
  def apply(): CacheConfig = new CacheConfig()
}
