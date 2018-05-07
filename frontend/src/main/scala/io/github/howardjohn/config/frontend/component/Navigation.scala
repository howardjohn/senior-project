package io.github.howardjohn.config.frontend.component

import io.github.howardjohn.config.frontend.external._
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.web.html.{className, href}

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
