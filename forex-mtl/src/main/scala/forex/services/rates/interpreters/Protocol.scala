package forex.services.rates.interpreters

import forex.services.rates.interpreters.errors.OneFrameServiceErrorResponse
import io.circe.Decoder
import io.circe.generic.semiauto._

import java.time.OffsetDateTime
import scala.util.Try

object Protocol {

  implicit val timestampDecoder: Decoder[OffsetDateTime] =
    Decoder.decodeString.emapTry((value: String) => Try(OffsetDateTime.parse(value)))

  case class ExternalRate(from: String, to: String, bid: BigDecimal, ask: BigDecimal, price: BigDecimal, time_stamp: String)

  object ExternalRate {
    implicit val externalRateDecoder: Decoder[ExternalRate] = Decoder.forProduct6(
      "from", "to", "bid", "ask", "price", "time_stamp"
    )(ExternalRate.apply)
  }
  implicit val errorResponseDecoder: Decoder[OneFrameServiceErrorResponse] = deriveDecoder[OneFrameServiceErrorResponse]
}
