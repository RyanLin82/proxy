package forex.programs.rates

import cats.effect.IO
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.programs.rates.errors.toProgramError
import forex.services.RatesService
import forex.services.rates.cache.CacheService
import forex.services.rates.errors.Error.CacheLookupFailed
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProgramSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "getRatePair" should "return rate 1 if from and to currencies are the same" in {
    val mockRatesService = mock[RatesService[IO]]
    val mockCacheService = mock[CacheService[IO]]

    val program = new Program[IO](mockRatesService, mockCacheService)

    val result = program.getRatePair(forex.programs.rates.Protocol.GetRatesRequest(Currency.USD, Currency.USD)).unsafeRunSync()

    result.map(
      rate => {
        rate.pair should be (Rate.Pair(Currency.USD, Currency.USD))
        rate.price should be (Price(BigDecimal(1)))
      }
    )
  }

  it should "retrieve rate from cache if available" in {
    val mockRatesService = mock[RatesService[IO]]
    val mockCacheService = mock[CacheService[IO]]
    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    val rate = Rate(pair, Price(BigDecimal(1.1)), Timestamp.now)

    when(mockCacheService.getRateFromCache(pair)).thenReturn(IO.pure(Some(rate)))

    val program = new Program[IO](mockRatesService, mockCacheService)

    val result = program.getRatePair(forex.programs.rates.Protocol.GetRatesRequest(Currency.USD, Currency.EUR)).unsafeRunSync()

    result should be (Right(rate))
    verify(mockCacheService).getRateFromCache(pair)
    verifyNoMoreInteractions(mockRatesService)
  }

  it should "return error when the cache miss" in {
    val mockRatesService = mock[RatesService[IO]]
    val mockCacheService = mock[CacheService[IO]]
    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    val error =toProgramError(CacheLookupFailed(500, "Given currency pair can't find the rate."))
    when(mockCacheService.getRateFromCache(pair)).thenReturn(IO.pure(None))

    val program = new Program[IO](mockRatesService, mockCacheService)
    val result = program.getRatePair(forex.programs.rates.Protocol.GetRatesRequest(Currency.USD, Currency.EUR)).unsafeRunSync()

    result should be (Left(error))
  }

  "updateRatesCache" should "store rates in the cache" in {
    val mockRatesService = mock[RatesService[IO]]
    val mockCacheService = mock[CacheService[IO]]
    val rates = List(
      Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(BigDecimal(1.1)), Timestamp.now),
      Rate(Rate.Pair(Currency.USD, Currency.GBP), Price(BigDecimal(1.2)), Timestamp.now)
    )

    when(mockRatesService.allSupportedCurrenciesRateLookup()).thenReturn(IO.pure(Right(rates)))
    when(mockCacheService.storeInRatesCache(rates)).thenReturn(IO.unit)

    val program = new Program[IO](mockRatesService, mockCacheService)

    program.updateRatesCache().unsafeRunSync()

    verify(mockCacheService).storeInRatesCache(rates)
  }

  "updateRatesCache" should "not store rates in the cache" in {
    val mockRatesService = mock[RatesService[IO]]
    val mockCacheService = mock[CacheService[IO]]
    val rates = List(
      Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(BigDecimal(1.1)), Timestamp.now),
      Rate(Rate.Pair(Currency.USD, Currency.GBP), Price(BigDecimal(1.2)), Timestamp.now)
    )

    when(mockRatesService.allSupportedCurrenciesRateLookup()).thenReturn(IO.pure(Left(mock[forex.services.rates.errors.Error])))
    when(mockCacheService.storeInRatesCache(rates)).thenReturn(IO.unit)

    val program = new Program[IO](mockRatesService, mockCacheService)

    program.updateRatesCache().unsafeRunSync()

    verify(mockCacheService, times(0)).storeInRatesCache(rates)
  }
}
