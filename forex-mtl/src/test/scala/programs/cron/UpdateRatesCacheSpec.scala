package programs.cron

import cats.effect.{ContextShift, IO, Timer}
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.programs.cron.UpdateRatesCache
import forex.services.rates.cache.CacheService
import forex.services.rates.interpreters.OneFrameForex
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext

class UpdateRatesCacheSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  "scheduledTask" should "call allSupportedCurrenciesRateLookup and storeInRatesCache" in {
    val mockCacheService = mock[CacheService[IO]]
    val mockOneFrameForex = mock[OneFrameForex[IO]]

    val mockRates = List(
      Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(BigDecimal(0.85)), Timestamp(OffsetDateTime.now()))
    )

    when(mockOneFrameForex.allSupportedCurrenciesRateLookup())
      .thenReturn(IO.pure(Right(mockRates)))

    when(mockCacheService.storeInRatesCache(any[List[Rate]]))
      .thenReturn(IO.unit)

    val test = UpdateRatesCache.scheduledTask(mockOneFrameForex, mockCacheService)

    test.unsafeRunSync()
    verify(mockCacheService).storeInRatesCache(mockRates)
  }
}
