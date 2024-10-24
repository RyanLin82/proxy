package forex.services.rates.interpreters

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.Algebra
import forex.services.rates.errors._
import forex.services.rates.interpreters.Protocol.ExternalRate
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
   * Looks up the exchange rate for a specific currency pair.
   *
   * @param pair The currency pair to look up.
   * @return Either an error or the exchange rate.
   */
  override def rateLookup(pair: Rate.Pair): F[Error Either Rate] = {
    val baseUri: Uri = Uri.unsafeFromString(baseUrl)
    val requestUri = baseUri.withQueryParam("pair", s"${pair.from}${pair.to}")

    val request = Request[F](
      method = Method.GET,
      uri = requestUri,
      headers = Headers(Header.Raw(CIString("token"), token))
    )

    // Log the request being sent
    logger.info(s"Sending rate lookup request for pair: ${pair.from}${pair.to} with URI: $requestUri")

    // Execute the HTTP request asynchronously and expect a List of ExternalRate
    client.expect[List[ExternalRate]](request).attempt.map {
      case Right(responseList) if responseList.nonEmpty =>
        val rate = parseExternalRateToRate(responseList.head)
        logger.info(s"Successfully fetched rate: $rate")
        rate.asRight[Error]

      case Right(_) =>
        // Handle case where the response list is empty
        logger.warn("Received empty response list for rate lookup.")
        Error.OneFrameLookupFailed("Empty response list").asLeft[Rate]

      case Left(error) =>
        logger.error(s"Error during rate lookup: ${error.getMessage}", error)
        Error.OneFrameLookupFailed(error.getMessage).asLeft[Rate]
    }
  }

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

    client.expect[List[ExternalRate]](request).attempt.map {
      case Right(responseList) if responseList.nonEmpty =>
        val result = responseList.map {response => parseExternalRateToRate(response)}
        logger.info(s"Successfully fetched ${result.size} rates.")
        result.asRight[Error]

      case Right(_) =>
        logger.warn("Received empty response list when fetching all currency pairs.")
        Error.OneFrameLookupFailed("Empty response list").asLeft[List[Rate]]

      case Left(error) =>
        logger.error(s"Error fetching all supported currency pairs: ${error.getMessage}", error)
        Error.OneFrameLookupFailed(error.getMessage).asLeft[List[Rate]]
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




