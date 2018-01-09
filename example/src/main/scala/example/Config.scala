package example

import com.typesafe.config.ConfigFactory
import pureconfig.loadConfigOrThrow

case class Config(
    serviceName: String,
    serviceVersion: String,
    awsRegion: String,
    destinationBucket: String)

object Config {
  def load: Config =
    loadConfigOrThrow[Config](ConfigFactory.load)
}
