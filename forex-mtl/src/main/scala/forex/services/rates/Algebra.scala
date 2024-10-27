package forex.services.rates

import forex.domain.Rate
import errors._

trait Algebra[F[_]] {
  def allSupportedCurrenciesRateLookup(): F[Error Either List[Rate]]
}
