package io.github.howardjohn.config.frontend

import io.github.howardjohn.config.frontend.external.Button.Props
import io.github.howardjohn.config.frontend.external._
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@react class App extends StatelessComponent {
  type Props = Unit

  def render: ReactElement =
    div(
      Navigation(),
      div(className := "container-fluid")(
        div(className := "row")(
          div(className := "col-3")(
            ListGroup()(
              ListGroupItem(active = true, action = true)("Namespace 1"),
              ListGroupItem(action = true)("Namespace 2")
            )
          ),
          div(className := "col-9")(
            "Select a namespace to view it"
          )
        )
      )
    )
}

@react class Navigation extends StatelessComponent {
  type Props = Unit

  def render =
    Navbar(dark = true, color = Some("primary"), expand = Some("md"))(className := "fixed-top")(
      NavbarBrand()(href := "#")("Dynamic Configuration"),
      Nav(navbar = true)(
        NavItem()(
          NavLink(active = true)(href := "#")("Namespaces")
        ),
        NavItem()(
          NavLink(active = true)(href := "#")("Tags")
        )
      )
    )
}
