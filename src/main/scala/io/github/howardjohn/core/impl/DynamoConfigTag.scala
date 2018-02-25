package io.github.howardjohn.core.impl

import cats.implicits._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import io.github.howardjohn.core.ConfigError.ReadError
import io.github.howardjohn.core.{Result, TagEntry}
import io.github.howardjohn.core.config.ConfigTag

class DynamoConfigTag(val namespace: String, val tagName: String, scanamo: Scanamo) extends ConfigTag {

  def getDetails(): Result[Option[TagEntry]] =
    scanamo
      .exec {
        Scanamo.tagsTable.get('namespace -> namespace and 'tag -> tagName)
      }
      .map(_.sequence)
      .map(o => o.left.map(e => ReadError(DynamoReadError.describe(e))))

}
