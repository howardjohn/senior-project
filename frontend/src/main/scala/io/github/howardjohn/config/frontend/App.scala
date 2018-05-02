package io.github.howardjohn.config.frontend

import io.circe.Json
import io.github.howardjohn.config.AuditInfo
import io.github.howardjohn.config.ConfigEntry
import io.github.howardjohn.config.frontend.action.Actions
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
    tagName: String,
    namespaces: Seq[String],
    activeNamespace: Seq[ConfigEntry[String]],
    error: Option[String] = None
  )

  def fetchNamespace(namespace: String): Unit =
    Actions.client
      .getNamespace[Json](namespace)
      .getTag(this.state.tagName)
      .getDetails()
      .flatMap { td =>
        Actions.client
          .getNamespace[Json](namespace)
          .getVersion(td.get.version)
          .getAll()
      }
      .value
      .unsafeRunAsync {
        case Right(Right(entries)) =>
          this.setState {
            _.copy(
              error = None,
              activeNamespace = entries.map(entry => entry.copy(value = entry.value.spaces4))
            )
          }
        case Left(err) =>
          this.setState(_.copy(error = Some(s"Couldn't fetch versions. Error: $err")))
        case Right(Left(err)) =>
          this.setState(_.copy(error = Some(s"Couldn't fetch versions. Error: $err")))
      }

  override def componentWillMount(): Unit = fetchNamespace(this.state.selectedNamespace)

  override def initialState: State =
    State(
      selectedNamespace = "example",
      tagName = "latest",
      namespaces = Seq("example", "Test"),
      activeNamespace = Seq(
        ConfigEntry(
          "key1",
          "version1",
          "blah",
          AuditInfo.default()
        ))
    )

  def handleClick(namespace: String)(e: Event): Unit = {
    this.setState(_.copy(selectedNamespace = namespace))
    this.fetchNamespace(namespace)
  }

  def render: ReactElement =
    div(
      Navigation(),
      div(className := "container-fluid")(
        div(className := "row")(
          div(className := "col-3")(
            ListGroup()(
              this.state.namespaces.map { name =>
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
            this.state.error match {
              case Some(err) => Alert("warning")(err)
              case None => NamespaceTable(contents = this.state.activeNamespace)
            }
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
