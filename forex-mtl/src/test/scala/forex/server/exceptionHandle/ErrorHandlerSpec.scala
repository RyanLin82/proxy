package forex.server.exceptionHandle

import cats.effect.IO
import forex.domain.ProxyException
import forex.http.jsonDecoder
import io.circe.Json
import org.http4s._
import org.http4s.implicits._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ErrorHandlerSpec extends AnyFunSuite with Matchers {

  private def extractJson(response: Response[IO]): Json = {
    response.as[Json].unsafeRunSync()
  }

  test("should handle ProxyException with InternalServerError") {
    val app = HttpApp[IO](_ => IO.raiseError(new ProxyException(502, "Proxy error occurred")))
    val errorHandlerApp = ErrorHandler(app)

    val request = Request[IO](Method.GET, uri"/test")
    val response = errorHandlerApp.run(request).unsafeRunSync()

    response.status shouldBe Status.InternalServerError

    val json = extractJson(response)
    json.hcursor.get[Int]("status").toOption shouldBe Some(500)
    json.hcursor.get[Int]("errorCode").toOption shouldBe Some(502)
    json.hcursor.get[String]("message").toOption shouldBe Some("Proxy error occurred")
  }

  test("should handle IllegalArgumentException with BadRequest") {
    val app = HttpApp[IO](_ => IO.raiseError(new IllegalArgumentException("Invalid argument")))
    val errorHandlerApp = ErrorHandler(app)

    val request = Request[IO](Method.GET, uri"/test")
    val response = errorHandlerApp.run(request).unsafeRunSync()

    response.status shouldBe Status.BadRequest

    val json = extractJson(response)
    json.hcursor.get[Int]("status").toOption shouldBe Some(400)
    json.hcursor.get[Int]("errorCode").toOption shouldBe Some(400)
    json.hcursor.get[String]("message").toOption shouldBe Some("Invalid argument")
  }

  test("should handle generic Exception with InternalServerError") {
    val app = HttpApp[IO](_ => IO.raiseError(new Exception("Generic error")))
    val errorHandlerApp = ErrorHandler(app)

    val request = Request[IO](Method.GET, uri"/test")
    val response = errorHandlerApp.run(request).unsafeRunSync()

    response.status shouldBe Status.InternalServerError

    val json = extractJson(response)
    json.hcursor.get[Int]("status").toOption shouldBe Some(500)
    json.hcursor.get[Int]("errorCode").toOption shouldBe Some(500)
    json.hcursor.get[String]("message").toOption shouldBe Some("Generic error")
  }
}
