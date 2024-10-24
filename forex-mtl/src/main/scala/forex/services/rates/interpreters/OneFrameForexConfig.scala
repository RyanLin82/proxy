package forex.services.rates.interpreters

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}


/**
 * Configuration class for the Forex client.
 * This class loads the API configurations such as base URL and token from the `forex-client.api` section
 * in the application's configuration file.
 *
 * @constructor Creates a new instance of OneFrameForexConfig.
 */
class OneFrameForexConfig {
  private val logger: Logger = LoggerFactory.getLogger(classOf[OneFrameForexConfig])
  private val config: Config = ConfigFactory.load()
  private val clientConfig: Config = config.getConfig("forex-client.api")

  /**
   * The base URL for the Forex client API.
   */
  val baseUrl: String = {
    val url = clientConfig.getString("base-url")
    logger.info(s"Loaded base URL for Forex client: $url")
    url
  }

  /**
   * The token used for authenticating with the Forex client API.
   */
  val token: String = {
    val apiToken = clientConfig.getString("token")
    logger.info("Loaded token for Forex client.")
    apiToken
  }
}

object OneFrameForexConfig {
  /**
   * Creates and returns a new instance of ForexClientConfig.
   *
   * @return A new ForexClientConfig instance.
   */
  def apply(): OneFrameForexConfig = new OneFrameForexConfig()
}