package forex.server.auth

import cats.effect.{ContextShift, IO, Timer}
import scala.concurrent.ExecutionContext.global
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, Headers, Request, Response}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.typesafe.config.ConfigFactory

class TokenAuthSpec extends AnyFlatSpec with Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  private val config = ConfigFactory.load()
  private val validToken = config.getConfig("auth").getString("token")
  private val invalidToken = "invalid-token"

  "TokenAuth" should "allow a request with a valid token" in {
    val request = Request[IO](
      method = Method.GET,
      uri = uri"/test",
      headers = Headers(Authorization(Credentials.Token(AuthScheme.Bearer, validToken)))
    )

    val response: Response[IO] = testService(TokenAuth.authMiddleware[IO]).run(request).unsafeRunSync()

    response.status shouldBe Status.Ok
  }

  it should "deny a request with an invalid token" in {
    val request = Request[IO](
      method = Method.GET,
      uri = uri"/test",
      headers = Headers(Authorization(Credentials.Token(AuthScheme.Bearer, invalidToken)))
    )

    val response: Response[IO] = testService(TokenAuth.authMiddleware[IO]).run(request).unsafeRunSync()

    response.status shouldBe Status.Unauthorized
  }

  it should "deny a request with no Authorization header" in {
    val request = Request[IO](method = Method.GET, uri = uri"/test")

    val response: Response[IO] = testService(TokenAuth.authMiddleware[IO]).run(request).unsafeRunSync()

    response.status shouldBe Status.Unauthorized
  }

  private def testService(authMiddleware: AuthMiddleware[IO, Unit]): HttpApp[IO] = {
    val authedRoutes = AuthedRoutes.of[Unit, IO] {
      case GET -> Root / "test" as _ => Ok("Authenticated")
    }

    authMiddleware(authedRoutes).orNotFound
  }
}
