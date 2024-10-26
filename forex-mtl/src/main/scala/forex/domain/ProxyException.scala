package forex.domain

class ProxyException(val code: Int, message: String, cause: Throwable = null) extends Exception(message, cause) {
  def this(code: Int, message: String) = this(code, message, null)
}
