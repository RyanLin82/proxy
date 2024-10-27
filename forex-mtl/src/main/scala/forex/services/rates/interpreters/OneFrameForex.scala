package forex.services.rates.interpreters

import cats.effect.{Async, Sync}
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.Algebra
import forex.services.rates.errors._
import forex.services.rates.interpreters.Protocol.{ErrorResponse, ExternalRate}
import io.circe.Json
import io.circe.generic.extras.decoding.ReprDecoder.deriveReprDecoder
import io.circe.parser.decode
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.typelevel.ci.CIString

import java.time.OffsetDateTime


class OneFrameForex[F[_]: Async](client: Client[F], oneFrameForexConfig: OneFrameForexConfig = OneFrameForexConfig()) extends Algebra[F] with LazyLogging {

  private def baseUrl = oneFrameForexConfig.baseUrl
  private def token = oneFrameForexConfig.token
  implicit val externalRateListEntityDecoder: EntityDecoder[F, List[ExternalRate]] = jsonOf[F, List[ExternalRate]]

  /**
   * Fetches the exchange rates for all supported currency pairs.
   *
   * @return Either an error or a list of exchange rates.
   */
  override def allSupportedCurrenciesRateLookup(): F[Error Either List[Rate]] = {
    val requestUri = buildUriForAllPairs()

    val request = Request[F](
      method = Method.GET,
      uri = requestUri,
      headers = Headers(Header.Raw(CIString("token"), token))
    )

    logger.info(s"Sending request to fetch all supported currency pairs with URI: $requestUri")

    val fetchRates = client.expect[Json](request).attempt.map {
      case Right(json) =>
        decode[List[ExternalRate]](json.noSpaces) match {
          case Right(responseList) if responseList.nonEmpty =>
            val result = responseList.map {response => parseExternalRateToRate(response)}
            logger.info(s"Successfully fetched ${result.size} rates.")
            result.asRight[Error]

          case Right(_) =>
            logger.warn("Received empty response list for rate lookup.")
            Error.OneFrameLookupFailed(500, "Empty response list").asLeft[List[Rate]]

          case Left(_) =>
            decode[ErrorResponse](json.noSpaces) match {
              case Right(errorResponse) =>
                logger.error(s"External service error: ${errorResponse.error}")
                Error.OneFrameLookupFailed(500, "External One Frame API encountered an issue").asLeft[List[Rate]]

              case Left(_) =>
                logger.error(s"Unexpected response format: $json")
                Error.OneFrameLookupFailed(500, "Unexpected response format from One Frame service").asLeft[List[Rate]]
            }
        }
      case Left(error) =>
        logger.error(s"Unexpected failure to fetch all supported currency pairs", error)
        Error.OneFrameLookupFailed(500, "Unexpected failure from one frame service").asLeft[List[Rate]]
    }

    fetchRates.handleErrorWith { error =>
      logger.error(s"Unexpected failure to fetch all supported currency pairs", error)
      Sync[F].pure(Error.OneFrameLookupFailed(503, "Unexpected failure from one frame service").asLeft[List[Rate]])
    }
  }

  /**
   * Converts an `ExternalRate` instance into a `Rate` domain model.
   *
   * @param externalRate The `ExternalRate` object containing raw rate data fetched from an external source.
   * @return A `Rate` object that maps the external rate data to the internal domain model.
   */
  private def parseExternalRateToRate(externalRate: ExternalRate): Rate = {
    Rate(
      Rate.Pair(Currency.fromString(externalRate.from), Currency.fromString(externalRate.to)),
      Price(externalRate.price),
      Timestamp(OffsetDateTime.parse(externalRate.time_stamp))
    )
  }

  /**
   * Builds the URI for fetching rates for all currency pairs.
   *
   * @return The constructed URI.
   */
  private def buildUriForAllPairs(): Uri = {
    val pairString = generatePairQueryString(Currency.getAllCurrency)
    val uri = Uri.unsafeFromString(s"$baseUrl?$pairString")
    logger.debug(s"Built URI for all pairs: $uri")
    uri
  }

  /**
   * Generates a query string for all possible currency pairs.
   *
   * @param currencies The list of currencies to create pairs from.
   * @return A query string representing all currency pairs.
   */
  private def generatePairQueryString(currencies: List[Currency]): String = {
    val pairs = for {
      from <- currencies
      to <- currencies if from != to
    } yield s"pair=${from.toString}${to.toString}"
    val queryString = pairs.mkString("&")
    logger.debug(s"Generated query string for all pairs: $queryString")
    queryString
  }
}

object OneFrameForex {
  /**
   * Creates an instance of `OneFrameForex` using the provided HTTP client and optional configuration.
   *
   * @param client The HTTP client to be used for making requests.
   * @param config The optional configuration. If not provided, a default configuration will be used.
   * @tparam F The effect type (e.g., IO).
   * @return An instance of `OneFrameForex[F]`.
   */
  def apply[F[_]: Async](client: Client[F], config: OneFrameForexConfig = OneFrameForexConfig()): OneFrameForex[F] = {
    new OneFrameForex(client, config)
  }
}






