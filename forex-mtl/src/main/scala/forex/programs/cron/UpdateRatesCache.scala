package forex.programs.cron

import cats.effect.{ConcurrentEffect, IO, Timer}
import cats.implicits.toFlatMapOps
import com.typesafe.scalalogging.LazyLogging
import forex.services.rates.cache.CacheService
import forex.services.rates.interpreters.OneFrameForex
import fs2.Stream
import org.http4s.client.Client

import scala.concurrent.duration._

/**
 * UpdateRatesCache is responsible for periodically fetching and storing exchange rates.
 * It contains the logic for scheduling and running the cron job.
 */
object UpdateRatesCache extends LazyLogging {

  /**
   * Fetches exchange rates for all supported currency pairs.
   *
   * @param oneFrameForex The oneFrameForex to use for fetching data.
   * @return An effect that will fetch the rates and handle success/failure scenarios.
   */
  def scheduledTask[F[_]: ConcurrentEffect](oneFrameForex: OneFrameForex[F], cacheService: CacheService[F]): F[Unit] = {
    logger.info("Executing scheduled task...")
    oneFrameForex.allSupportedCurrenciesRateLookup().flatMap {
        case Right(rates) =>
            cacheService.storeInRatesCache(rates)
        case Left(error) =>
          IO(logger.error(s"Failed to fetch rates: $error")).to[F]
      }
  }

  /**
   * Creates a cron job that runs the `scheduledTask` every given minutes.
   *
   * @param client The HTTP client to use for fetching data.
   * @return A stream that runs the cron job periodically.
   */
  def cronJob[F[_]: ConcurrentEffect: Timer](client: Client[F], cacheService: CacheService[F]): Stream[F, Unit] = {
    logger.info("Starting cron job stream...")
    Stream.awakeEvery[F](UpdateRatesCronJobConfig().cronTime.minutes).evalMap { _ =>
      logger.info("Cron job triggered. Fetching and storing rates...")
      scheduledTask(OneFrameForex[F](client), cacheService)
    }
  }
}