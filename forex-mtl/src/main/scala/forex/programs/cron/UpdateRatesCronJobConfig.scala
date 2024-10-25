package forex.programs.cron

import com.typesafe.config.ConfigFactory

class UpdateRatesCronJobConfig {
  val cronTime: Long = ConfigFactory.load().getConfig("cron").getLong("time")
}

object UpdateRatesCronJobConfig {
  def apply(): UpdateRatesCronJobConfig = new UpdateRatesCronJobConfig()
}
