package forex.services.rates.cache

import forex.domain.{Price, Timestamp}

case class RateCacheValue(
                           price: Price,
                           timestamp: Timestamp
                         )
