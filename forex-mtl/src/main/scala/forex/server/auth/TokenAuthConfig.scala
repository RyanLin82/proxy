package forex.server.auth

import com.typesafe.config.{Config, ConfigFactory}

class TokenAuthConfig {

  private val config: Config = ConfigFactory.load()
  private val clientConfig: Config = config.getConfig("auth")

  /**
   * The base URL for the Forex client API.
   */
  val baseUrl: String = {
    clientConfig.getString("token")
  }
}
object OneFrameForexConfig {
  /**
   * Creates and returns a new instance of ForexClientConfig.
   *
   * @return A new ForexClientConfig instance.
   */
  def apply(): TokenAuthConfig = new TokenAuthConfig()
}

