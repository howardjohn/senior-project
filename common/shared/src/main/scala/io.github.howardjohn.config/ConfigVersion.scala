package io.github.howardjohn.config

/**
 * A ConfigVersion identifies a (potentially immutable) set of values in a given namespace.
 *
 * Version can be marked as frozen to make them immutable. The recommended workflow is to create a version, make all
 * necessary changes, then freeze the version before use. However, there are no barriers to querying a unfrozen version.
 *
 * @tparam T the type of the records stored in the namespace.
 */
trait ConfigVersion[T] {

  /**
   * @return the name of the version.
   */
  def version: String

  /**
   * @return the namespace this version refers to.
   */
  def namespace: String

  /**
   * @return the details for the version.
   */
  def details(): Result[Option[VersionEntry]]

  /**
   * This marks the version as immutable. Once this has happened, no changes can be made to the version, and it cannot
   * be unfrozen.
   * @return the error, if one occurred.
   */
  def freeze(): Result[Unit]

  /**
   * This operation copies all contents of a version into a new version.
   * The new version will always be unfrozen initially.
   *
   * @param newVersionName the name for the new version.
   * @return the newly created version.
   */
  def cloneVersion(newVersionName: String): Result[ConfigVersion[T]]

  /**
   * @param key the key to lookup.
   * @return the entry identified by the given key.
   */
  def get(key: String): Result[Option[ConfigEntry[T]]]

  /**
   * @return a list of all entries in the version.
   */
  def getAll(): Result[Seq[ConfigEntry[T]]]

  /**
   * Puts a new entry in the version. If the key already exists, this will overwrite the existing value.
   * Note that this will fail if the version is frozen.
   *
   * @param key the key for the new entry.
   * @param value the value for the new entry.
   * @return the error, if any.
   */
  def write(key: String, value: T): Result[Unit]

  /**
   * Deletes a record identified by key.
   * @param key the key of the entry to delete.
   * @return the error, if any.
   */
  def delete(key: String): Result[Unit]
}
