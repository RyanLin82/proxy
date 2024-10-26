package forex

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import forex.config._
import forex.programs.RatesProgram
import forex.programs.cron.UpdateRatesCache
import forex.services.rates.cache.CacheService
import fs2.Stream
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] extends LazyLogging{

  def stream(ec: ExecutionContext): Stream[F, Unit] = {
    for {
      config <- Config.stream("app")
      client <- BlazeClientBuilder[F](ec).stream // Create the http4s client
      cacheService = CacheService[F]()
      module = new Module[F](config, client, cacheService) // Pass the client to the Module
      _ <- Stream.eval(runOnStartup(module.ratesProgram))
      _ <- BlazeServerBuilder[F](ec)
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(module.httpApp)
        .serve
        .concurrently(UpdateRatesCache.cronJob(client, cacheService))
    } yield ()
  }

    /**
     * Task to run once when the server starts.
     */
    private def runOnStartup(program: RatesProgram[F]): F[Unit] = {
      logger.info("Server is starting, running initialization task...")
      program.updateRatesCache()
    }
}
