package forex.server.exceptionHandle

import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeError
import forex.domain.ProxyException
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

/**
 * ErrorHandler middleware to catch and handle exceptions during HTTP request processing.
 *
 * It intercepts specific exceptions (like `ProxyException` and `IllegalArgumentException`) and generates
 * appropriate HTTP responses with a consistent JSON error structure. Other generic exceptions are also handled.
 */
object ErrorHandler {

  /**
   * Applies error handling to an existing HttpApp.
   *
   * @param httpApp The existing HttpApp to wrap with error handling.
   * @tparam F The effect type, which must support Sync operations.
   * @return An HttpApp with integrated error handling that produces standardized error responses.
   */
  def apply[F[_]: Sync](httpApp: HttpApp[F]): HttpApp[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpApp[F] { req =>
      httpApp.run(req).handleErrorWith {
        case e: ProxyException =>
          InternalServerError(generateErrorResponse(Status.InternalServerError.code, e.code, e.getMessage))
        case e: IllegalArgumentException =>
          BadRequest(generateErrorResponse(Status.BadRequest.code, Status.BadRequest.code, e.getMessage))
        case e: Exception =>
          InternalServerError(generateErrorResponse(Status.InternalServerError.code, Status.InternalServerError.code, e.getMessage))
      }
    }
  }

  /**
   * Generates a JSON response object for errors.
   *
   * @param status The HTTP status code to include in the response.
   * @param errorCode The application-specific error code.
   * @param errorMsg The error message to display.
   * @return A JSON object representing the error response.
   */
  private def generateErrorResponse(status: Int, errorCode: Int, errorMsg: String): Json = {
    Json.obj(
      "status" -> Json.fromInt(status),
      "errorCode" -> Json.fromInt(errorCode),
      "message" -> Json.fromString(errorMsg)
    )
  }
}
