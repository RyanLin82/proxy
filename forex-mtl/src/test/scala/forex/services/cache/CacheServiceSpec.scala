package forex.services.cache

import cats.effect.IO
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.cache.CacheService
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class CacheServiceSpec extends AsyncWordSpec with Matchers {

  "CacheService" should {
    "store and retrieve rates correctly" in {
      val cacheService = CacheService[IO]()
      val pair = Rate.Pair(Currency.USD, Currency.EUR)
      val rate = Rate(pair, Price(BigDecimal(1.2)), Timestamp.now)

      val test = for {
        _ <- cacheService.storeInRatesCache(List(rate))
        retrievedRate <- cacheService.getRateFromCache(pair)
      } yield {
        retrievedRate should contain(rate)
      }

      test.unsafeToFuture()
    }
  }
}
