package forex

import cats.data.Kleisli
import cats.effect.{Concurrent, Timer}
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.server.auth.TokenAuth
import forex.services._
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.{AutoSlash, Timeout}

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig, client: Client[F]) {

  private val ratesService: RatesService[F] = RatesServices.dummy[F](client)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  private type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  private type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val authMiddleware: AuthMiddleware[F, Unit] = TokenAuth.authMiddleware

  private val secureRoutes: HttpRoutes[F] = authMiddleware {
    Kleisli { req =>
      ratesHttpRoutes.run(req.req)
    }
  }

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(secureRoutes).orNotFound)

}
