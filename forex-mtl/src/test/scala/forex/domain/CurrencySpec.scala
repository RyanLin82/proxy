package forex.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class CurrencySpec extends AnyFlatSpec with Matchers {

  "Currency.fromString" should "return the correct Currency object for valid strings" in {
    Currency.fromString("USD") should be (Currency.USD)
    Currency.fromString("EUR") should be (Currency.EUR)
    Currency.fromString("JPY") should be (Currency.JPY)
  }

  it should "throw an IllegalArgumentException for unknown currencies" in {
    assertThrows[IllegalArgumentException] {
      Currency.fromString("XYZ")
    }
  }

  "Currency.getAllCurrency" should "return a list of all supported currencies" in {
    val expectedCurrencies = List(
      Currency.AUD, Currency.CAD, Currency.CHF, Currency.EUR,
      Currency.GBP, Currency.NZD, Currency.JPY, Currency.SGD, Currency.USD
    )
    Currency.getAllCurrency should contain theSameElementsAs expectedCurrencies
  }
}