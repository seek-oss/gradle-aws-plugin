package seek.aws

import com.amazonaws.regions.Regions

class AwsPluginExtension {
  private[aws] var region: Regions = Regions.AP_SOUTHEAST_2

  def setRegion(r: String): Unit =
    region = Regions.fromName(r)
}
