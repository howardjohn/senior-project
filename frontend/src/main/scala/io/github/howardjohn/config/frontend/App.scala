package io.github.howardjohn.config.frontend

import cats.effect.IO
import io.circe.Json
import io.github.howardjohn.config.AuditInfo
import io.github.howardjohn.config.ConfigEntry
import io.github.howardjohn.config.frontend.action.Actions
import io.github.howardjohn.config.frontend.component.{NamespaceTable, Navigation}
import io.github.howardjohn.config.frontend.external._
import org.scalajs.dom.Event
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import cats.implicits._

@react class App extends Component {
  type Props = Unit

  case class State(
    selectedNamespace: Option[String] = None,
    tagName: String = "",
    namespaces: Seq[String] = Seq.empty,
    activeNamespace: Seq[ConfigEntry[String]] = Seq.empty,
    error: Option[String] = None
  )

  override def initialState: State = State()

  def fetchNamespace(namespace: String): IO[Seq[ConfigEntry[String]]] =
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
      .flatMap {
        case Right(entries) =>
          IO {
            val stringEntries = entries.map(entry => entry.copy(value = entry.value.spaces4))
            this.setState {
              _.copy(
                error = None,
                activeNamespace = stringEntries
              )
            }
            stringEntries
          }
        case Left(err) =>
          IO {
            this.setState(_.copy(error = Some(s"Couldn't fetch versions. Error: $err")))
            Seq.empty
          }
      }

  def fetchNamespaces(): IO[Seq[String]] =
    Actions.client
      .getAllNamespaces()
      .value
      .flatMap {
        case Right(newNamespaces) =>
          IO {
            this.setState {
              _.copy(
                error = None,
                namespaces = newNamespaces,
                selectedNamespace = newNamespaces.headOption
              )
            }
            newNamespaces
          }
        case Left(err) =>
          IO {
            this.setState(_.copy(error = Some(s"Couldn't fetch namespaces. Error: $err")))
            Seq()
          }
      }

  override def componentWillMount(): Unit =
    fetchNamespaces()
      .flatMap { ns =>
        ns.headOption.traverse(fetchNamespace)
      }
      .unsafeRunAsync(recordError)

  private def recordError[T](d: Either[Throwable, T]): Unit = d match {
    case Right(_) => ()
    case Left(err) => this.setState(_.copy(error = Some(s"Unknown error: $err")))
  }

  def handleClick(namespace: String)(e: Event): Unit = {
    for {
      _ <- this.fetchNamespace(namespace)
      _ <- IO(this.setState(_.copy(selectedNamespace = Some(namespace))))
    } yield ()
  }.unsafeRunAsync(recordError)

  def render: ReactElement =
    div(
      Navigation(),
      div(className := "container-fluid")(
        div(className := "row")(
          div(className := "col-3")(
            ListGroup()(
              this.state.namespaces.map { name =>
                ListGroupItem(
                  active = this.state.selectedNamespace == Some(name),
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
