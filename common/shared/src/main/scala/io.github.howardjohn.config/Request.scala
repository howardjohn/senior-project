package io.github.howardjohn.config

import io.circe.Json

object Request {
  case class CreateNamespaceRequest(
    namespace: String
  )

  case class CreateTagRequest(
    tag: String,
    namespace: String
  )

  case class MoveTagRequest(
    version: String,
    weight: Int
  )

  case class CreateVersionRequest(
    version: String
  )

  case class FreezeVersionRequest(
    frozen: Boolean
  )

  case class ConfigRequest(
    key: String,
    value: Json
  )
}
