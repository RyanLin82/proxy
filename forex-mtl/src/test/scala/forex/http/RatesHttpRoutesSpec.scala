package forex.http

import cats.effect.IO
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.rates.RatesHttpRoutes
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import org.http4s.{Method, Request, Status, Uri}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RatesHttpRoutesSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "RatesHttpRoutes" should {
    "return 200 OK for a successful GET request" in {
      val mockRatesProgram = mock[RatesProgram[IO]]
      val rate = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(BigDecimal(1.1)), Timestamp.now)

      when(mockRatesProgram.getRatePair(any[RatesProgramProtocol.GetRatesRequest]))
        .thenReturn(IO.pure(Right(rate)))

      val routes = new RatesHttpRoutes[IO](mockRatesProgram).routes
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString("/rates?from=USD&to=EUR"))

      val response = routes.orNotFound.run(request).unsafeRunSync()
      response.status shouldBe Status.Ok
    }

    "return 500 InternalServerError for a failed POST request" in {
      val mockRatesProgram = mock[RatesProgram[IO]]

      when(mockRatesProgram.updateRatesCache()).thenReturn(IO.raiseError(new Exception("Failed to update cache")))

      val routes = new RatesHttpRoutes[IO](mockRatesProgram).routes
      val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString("/rates/cache/refresh"))
      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.InternalServerError
    }

    "return 200 OK for a successful POST request" in {
      val mockRatesProgram = mock[RatesProgram[IO]]
      when(mockRatesProgram.updateRatesCache()).thenReturn(IO.unit)

      val routes = new RatesHttpRoutes[IO](mockRatesProgram).routes
      val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString("/rates/cache/refresh"))
      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok
    }
  }
}
