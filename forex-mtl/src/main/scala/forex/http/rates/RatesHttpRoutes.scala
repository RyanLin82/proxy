package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import forex.domain.ProxyException
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import Converters._
import Protocol._
import QueryParams._

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {



  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates.getRatePair(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
        case Right(rate) => Ok(rate.asGetApiResponse)
        case Left(RateLookupFailed(code, message)) => throw new ProxyException(code, message)
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
