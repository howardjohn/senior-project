package io.github.howardjohn.config.frontend.component

import io.circe.Json
import io.github.howardjohn.config.ConfigEntry
import io.github.howardjohn.config.frontend.external.{BootstrapTable, TableHeaderColumn}
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement

import scala.scalajs.js
import js.JSConverters._

@react class NamespaceTable extends StatelessComponent {
  case class Props(contents: Seq[ConfigEntry[String]])

  def contentsAsJs: js.Array[js.Any] = this.props.contents.map(js.use(_).as[js.Any]).toJSArray

  def render: ReactElement =
    BootstrapTable(contentsAsJs, striped = true, hover = true)(
      TableHeaderColumn(isKey = true, dataField = "key")("Key"),
      TableHeaderColumn(dataField = "version")("Version"),
      TableHeaderColumn(dataField = "value")("Value")
    )
}
