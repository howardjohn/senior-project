package io.github.howardjohn.config.frontend.external

import slinky.core.{ExternalComponent, ExternalComponentWithAttributes, ReactComponentClass}
import slinky.core.annotations.react
import slinky.web.html.{a, div}

import scala.scalajs.js
import scala.scalajs.js.{|, Function, UndefOr}
import scala.scalajs.js.annotation.JSImport

@JSImport("reactstrap", JSImport.Default)
@js.native
object Reactstrap extends js.Object {
  val Button: js.Object = js.native
  val Navbar: js.Object = js.native
  val Nav: js.Object = js.native
  val NavbarBrand: js.Object = js.native
  val NavItem: js.Object = js.native
  val NavLink: js.Object = js.native
  val ListGroup: js.Object = js.native
  val ListGroupItem: js.Object = js.native
  val Alert: js.Object = js.native
}

@react object Button extends ExternalComponent {
  case class Props(
    active: Boolean = false,
    block: Boolean = false,
    color: String = "secondary",
    disabled: Boolean = false,
    tag: Option[Function | String] = None,
    innerRef: Option[Function | String] = None,
    onClick: Option[Function] = None,
    size: Option[String] = None
  )
  override val component = Reactstrap.Button
}
@react object Navbar extends ExternalComponentWithAttributes[div.tag.type] {
  case class Props(
    dark: Boolean = false,
    color: Option[String] = None,
    expand: Option[String] = None
  )
  override val component = Reactstrap.Navbar
}

@react object Nav extends ExternalComponent {
  case class Props(navbar: Boolean = false)
  override val component = Reactstrap.Nav
}

@react object NavbarBrand extends ExternalComponentWithAttributes[a.tag.type] {
  case class Props()
  override val component = Reactstrap.NavbarBrand
}

@react object NavItem extends ExternalComponent {
  case class Props(active: Boolean = false)
  override val component = Reactstrap.NavItem
}

@react object NavLink extends ExternalComponentWithAttributes[a.tag.type] {
  case class Props(active: Boolean = false)
  override val component = Reactstrap.NavLink
}

@react object ListGroup extends ExternalComponent {
  case class Props()
  override val component = Reactstrap.ListGroup
}

@react object Alert extends ExternalComponent {
  case class Props(color: String = "success")
  override val component = Reactstrap.Alert
}

@react object ListGroupItem extends ExternalComponentWithAttributes[a.tag.type] {
  case class Props(
    active: Boolean = false,
    action: Boolean = false
  )
  override val component = Reactstrap.ListGroupItem
}
