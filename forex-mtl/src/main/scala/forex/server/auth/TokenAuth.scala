package forex.server.auth

import cats.data.{Kleisli, OptionT}
import cats.effect.Concurrent
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.http4s.headers.Authorization
import org.http4s.implicits.http4sHeaderSyntax
import org.http4s.server.AuthMiddleware
import org.http4s.{Credentials, Request}


/**
 * Object providing token-based authentication middleware.
 */
object TokenAuth extends LazyLogging {

  private val config: Config = ConfigFactory.load()
  private val token: String = config.getConfig("auth").getString("token")

  /**
   * Validates the provided token against the expected value.
   *
   * @return true if the token is valid, false otherwise.
   */
  private def validateToken(providedToken: String): Boolean = {
    token == providedToken
  }

  /**
   * Kleisli that checks the token from the request headers and allows the request to proceed
   * only if the token matches the expected value.
   *
   * @tparam F The effect type.
   * @return Kleisli function for validating the token from the request.
   */
  private def authUser[F[_]: Concurrent]: Kleisli[OptionT[F, *], Request[F], Unit] = Kleisli { req =>
    val isValidToken = for {
      authHeader <- req.headers.get[Authorization]
      Credentials.Token(_, providedToken) <- Some(authHeader.credentials)
      if validateToken(providedToken)
    } yield ()

    if (isValidToken.isEmpty) {
      logger.warn(s"Invalid or missing token in request: ${req.headers.get[Authorization].map(_.value).getOrElse("No Authorization header")}")
    }

    OptionT.fromOption[F](isValidToken)
  }

  /**
   * Creates an authentication middleware that can be used to secure routes.
   *
   * @tparam F The effect type.
   * @return An AuthMiddleware that validates tokens.
   */
  def authMiddleware[F[_]: Concurrent]: AuthMiddleware[F, Unit] =
    AuthMiddleware(authUser)
}
