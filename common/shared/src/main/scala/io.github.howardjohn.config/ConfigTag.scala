package io.github.howardjohn.config

/**
 * A tag is a pointer to one or more versions. This allows an application to reference a tag that can be updated to
 * new versions without having to update the application. A tag is scoped to a namespace, but the same tag name can be
 * use in multiple namespaces, giving the appearance that they are global.
 * A tag can point to multiple versions by assigning a weight to each version. For example, if we assign version A with
 * weight 1 and version B with weight 3, then 75% of requests will return version B, with the remaining 25% returning
 * version A. This is based on a discriminator that is passed in; it is not random.
 */
trait ConfigTag {

  /**
   * @return the name of the tag.
   */
  def tagName: String

  /**
   * @return the namespace this tag applies to.
   */
  def namespace: String

  /**
   * Fetches the information for a tag. Note that this will fail if the tag has multiple versions pointed to.
   * @return the tag details.
   */
  def getDetails(): Result[Option[TagEntry]]

  /**
   * Fetches the information for a tag. The version is selected by using the discriminator. To get an accurate
   * distribution of versions that matches the weights, this value should be evenly distributed.
   * @param discriminator to select the version.
   * @return the tag details, with the selected version.
   */
  def getDetails(discriminator: String): Result[Option[TagEntry]]

  /**
   * Changes a tag to point to the given versions with given weights. This performs an update on the tag, it does not
   * replace the tag. This means to no longer point to a version, its weight should be set to 0.
   * @param versions a map of version to weight
   * @return the error, if any
   */
  def moveTag(versions: Map[String, Int]): Result[Unit]
}
