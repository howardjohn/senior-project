package io.github.howardjohn.core.impl

import io.github.howardjohn.core.AuditInfo

case class DynamoTagEntry(
  tag: String,
  namespace: String,
  versions: Map[String, Int],
  auditInfo: AuditInfo
)
