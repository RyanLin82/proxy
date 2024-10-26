package forex.services.rates.interpreters

object errors {
  sealed trait Error
  object Error {
    final case class OneFrameLookupFailed(code: Int =  500, msg: String) extends Error
  }
}
