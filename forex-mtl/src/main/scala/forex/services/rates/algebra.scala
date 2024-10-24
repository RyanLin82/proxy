package forex.services.rates

import forex.domain.Rate
import errors._

trait Algebra[F[_]] {
  def rateLookup(pair: Rate.Pair): F[Error Either Rate]
  def allSupportedCurrenciesRateLookup(): F[Error Either List[Rate]]
}
