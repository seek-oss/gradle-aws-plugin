package seek.aws

import com.amazonaws.regions.Regions

private[aws] class AwsPluginExtension {
  private[aws] var region: Regions = Regions.AP_SOUTHEAST_2
  private[aws] var profile: String = "default"
  private[aws] var lookupPrefix: String = "environment"

  def setRegion(r: String): Unit =
    region = Regions.fromName(r)

  def setProfile(p: String): Unit =
    profile = p

  def setLookupPrefix(p: String): Unit =
    lookupPrefix = p
}
