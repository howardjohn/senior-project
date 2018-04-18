package io.github.howardjohn.config.frontend

import slinky.core._
import slinky.core.annotations.react
import slinky.web.html._

@react class App extends StatelessComponent {
  type Props = Unit

  def render =
    h1(s"Hello World")
}
