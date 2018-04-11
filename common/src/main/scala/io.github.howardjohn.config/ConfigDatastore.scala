package io.github.howardjohn.config

/**
 * The ConfigDatastore defines the entry point into the config, giving access to top level functionality. All methods
 * that can fail return a type of Result. This allows callers to defer execution (IO) and exposes errors (Either).
 * @tparam B the typeclass that is required for the type of the records.
 */
trait ConfigDatastore[B[_]] {

  /**
   * Gets an instance for the given namespace.
   *
   * @param namespace the name of the namespace.
   * @tparam T the type of the records stored in the namespace.
   * @return the requested namespace.
   */
  def getNamespace[T: B](namespace: String): ConfigNamespace[T]

  /**
   * Creates a new namespace.
   *
   * @param namespace the name of the namespace.
   * @tparam T the type of the records that will be stored in the namespace.
   * @return the newly created namespace.
   */
  def createNamespace[T: B](namespace: String): Result[ConfigNamespace[T]]
}
