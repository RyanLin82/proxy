package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    final case class OneFrameLookupFailed(code: Int, msg: String) extends Error
  }

}
