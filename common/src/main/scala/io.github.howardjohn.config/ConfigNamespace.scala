package io.github.howardjohn.config

/**
 * A namespace logically groups together records. All entries in a namespace have the same types, and, although not
 * required, likely share the same key schema.
 *
 * @tparam T the type of the records stored in the namespace.
 */
trait ConfigNamespace[T] {

  /**
   * @return the name of the namespace.
   */
  def namespace: String

  /**
   * Gets an instance of a tag for this namespace.
   *
   * @param tag the name of the tag.
   * @return the requested tag.
   */
  def getTag(tag: String): ConfigTag

  /**
   * Creates a new tag for this namespace. The new tag will initially not point to any version.
   *
   * @param tag the name of the tag.
   * @return the newly created tag.
   */
  def createTag(tag: String): Result[ConfigTag]

  /**
   * @return all the versions for this namespace.
   */
  def getVersions(): Result[Seq[VersionEntry]]

  /**
   * @param version the name of the version.
   * @return the requested version.
   */
  def getVersion(version: String): ConfigVersion[T]

  /**
   * Creates a new version for this namespace. This version will initially be unfrozen.
   *
   * @param version the name of the new version.
   * @return the newly created version.
   */
  def createVersion(version: String): Result[ConfigVersion[T]]
}
