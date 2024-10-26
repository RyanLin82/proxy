package forex.programs.rates

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import forex.domain.Rate.Pair
import forex.domain._
import forex.programs.rates.errors._
import forex.services.RatesService
import forex.services.rates.cache.CacheService

class Program[F[_]: Sync](
    ratesService: RatesService[F],
    cacheService: CacheService[F]
) extends Algebra[F] with LazyLogging {

  override def getRatePair(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val pair = Pair(request.from, request.to)

    if (request.from == request.to) {
      Sync[F].pure(Rate(pair, forex.domain.Price(BigDecimal(1)), forex.domain.Timestamp.now).asRight[Error])
    } else {
      cacheService.getRateFromCache(pair).flatMap {
        case Some(rate) =>
          rate.asRight[Error].pure[F]
        case None =>
          ratesService.allSupportedCurrenciesRateLookup().flatMap {
            case Right(rates) =>
              cacheService.storeInRatesCache(rates).attempt.void
            case Left(_) =>
              ().pure[F]
          }.flatMap { _ =>
            EitherT(ratesService.rateLookup(pair)).leftMap(toProgramError).value
          }
      }
    }
  }

  override def updateRatesCache(): F[Unit] = {
    ratesService.allSupportedCurrenciesRateLookup().flatMap {
      case Right(rates) =>
        cacheService.storeInRatesCache(rates)
      case Left(error) =>
        Sync[F].delay(logger.error(s"Failed to fetch rates: $error"))
    }
  }
}

object Program {
  def apply[F[_]: Sync](
      ratesService: RatesService[F],
      cacheService: CacheService[F]
  ): Algebra[F] = new Program[F](ratesService, cacheService)
}

