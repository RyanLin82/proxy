package forex.domain

import cats.Show

/**
 * Represents a currency used in Forex trading.
 */
sealed trait Currency

/**
 * Companion object containing all currency definitions and utilities for conversion and display.
 */
object Currency {

  // Define all supported currencies as case objects
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency

  /**
   * Implicit Show instance to convert Currency to its string representation.
   */
  implicit val show: Show[Currency] = Show.show {
    case AUD => "AUD"
    case CAD => "CAD"
    case CHF => "CHF"
    case EUR => "EUR"
    case GBP => "GBP"
    case NZD => "NZD"
    case JPY => "JPY"
    case SGD => "SGD"
    case USD => "USD"
  }

  /**
   * Converts a string to its corresponding Currency.
   *
   * @param s The string representation of the currency (e.g., "USD").
   * @return The corresponding Currency object.
   * @throws IllegalArgumentException If the string does not match any supported currency.
   */
  def fromString(s: String): Currency = s.toUpperCase match {
    case "AUD" => AUD
    case "CAD" => CAD
    case "CHF" => CHF
    case "EUR" => EUR
    case "GBP" => GBP
    case "NZD" => NZD
    case "JPY" => JPY
    case "SGD" => SGD
    case "USD" => USD
    case unknown => throw new IllegalArgumentException(s"Unknown currency: $unknown")
  }

  /**
   * Retrieves a list of all supported Currency objects.
   *
   * @return A list containing all supported currencies.
   */
  def getAllCurrency: List[Currency] = List(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)
}
