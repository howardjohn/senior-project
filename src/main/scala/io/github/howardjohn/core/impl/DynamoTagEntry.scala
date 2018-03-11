package io.github.howardjohn.core.impl

import io.github.howardjohn.core.AuditInfo

case class DynamoTagEntry(
  tag: String,
  namespace: String,
  versions: Seq[DynamoTagEntryVersion],
  auditInfo: AuditInfo
)

case class DynamoTagEntryVersion(
  weight: Int,
  version: String
)