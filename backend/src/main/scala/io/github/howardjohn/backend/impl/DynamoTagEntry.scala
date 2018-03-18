package io.github.howardjohn.backend.impl

import io.github.howardjohn.backend.AuditInfo

case class DynamoTagEntry(
  tag: String,
  namespace: String,
  versions: Map[String, Int],
  auditInfo: AuditInfo
)
