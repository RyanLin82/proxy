package forex.services.rates

import cats.effect.Async
import forex.services.rates.interpreters._
import org.http4s.client.Client

object Interpreters {
  def oneFrameInterpreter[F[_]: Async](client: Client[F]): Algebra[F] = new OneFrameForex[F](client)
}
