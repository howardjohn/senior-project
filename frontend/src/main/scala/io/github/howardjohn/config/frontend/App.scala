package io.github.howardjohn.config.frontend

import io.github.howardjohn.config.{AuditInfo, ConfigEntry}
import io.github.howardjohn.config.frontend.component.NamespaceTable
import io.github.howardjohn.config.frontend.external._
import org.scalajs.dom.Event
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

@react class App extends Component {
  type Props = Unit
  case class State(
    selectedNamespace: String,
    namespaces: Map[String, Seq[ConfigEntry[String]]]
  )

  def initialState: State = State(
    selectedNamespace = "example",
    namespaces = Map(
      "example" -> Seq(
        ConfigEntry(
          "key1",
          "version1",
          "someconfig",
          AuditInfo.default()
        )),
      "Test" -> Seq.empty
    )
  )

  def handleClick(namespace: String)(e: Event): Unit = {
    this.setState(_.copy(selectedNamespace = namespace))
  }

  def render: ReactElement =
    div(
      Navigation(),
      div(className := "container-fluid")(
        div(className := "row")(
          div(className := "col-3")(
            ListGroup()(
              this.state.namespaces.keys.map { name =>
                ListGroupItem(
                  active = this.state.selectedNamespace == name,
                  action = true
                )(
                  key := name,
                  onClick := handleClick(name) _
                )(name)
              }
            )
          ),
          div(className := "col-9")(
            NamespaceTable(contents = this.state.namespaces(this.state.selectedNamespace))
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
