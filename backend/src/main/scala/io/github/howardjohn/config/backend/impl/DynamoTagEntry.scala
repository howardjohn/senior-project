package io.github.howardjohn.config.backend.impl

import io.github.howardjohn.config.AuditInfo

case class DynamoTagEntry(
  tag: String,
  namespace: String,
  versions: Map[String, Int],
  auditInfo: AuditInfo
)
