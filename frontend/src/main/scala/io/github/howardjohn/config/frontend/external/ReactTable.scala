package io.github.howardjohn.config.frontend.external

import slinky.core.ExternalComponent
import slinky.core.annotations.react

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
@JSImport("react-bootstrap-table", JSImport.Default)
@js.native
object ReactTable extends js.Object {
  val BootstrapTable: js.Object = js.native
  val TableHeaderColumn : js.Object = js.native
}

@react object BootstrapTable extends ExternalComponent {
  case class Props(
    data: js.Array[js.Any],
    striped: Boolean = false,
    hover: Boolean = false,
    version: String = "4"
  )
  override val component = ReactTable.BootstrapTable
}
@react object TableHeaderColumn extends ExternalComponent {
  case class Props(
    isKey: Boolean = false,
    dataField: String
  )
  override val component = ReactTable.TableHeaderColumn
}

