package io.github.howardjohn.config.frontend

import org.scalajs.dom
import slinky.hot
import slinky.web.ReactDOM

import scala.scalajs.{js, LinkingInfo}
import scala.scalajs.js.annotation.{JSExportTopLevel, JSImport}
@JSImport("resources/index.css", JSImport.Default)
@js.native
object IndexCSS extends js.Object

@JSImport("bootstrap/dist/css/bootstrap.css", JSImport.Default)
@js.native
object BootstrapCSS extends js.Object

object Main {
  private val bootstrapCss = BootstrapCSS
  private val css = IndexCSS

  @JSExportTopLevel("entrypoint.main")
  def main(): Unit = {
    if (LinkingInfo.developmentMode) {
      hot.initialize()
    }

    val container = Option(dom.document.getElementById("root")).getOrElse {
      val elem = dom.document.createElement("div")
      elem.id = "root"
      dom.document.body.appendChild(elem)
      elem
    }

    ReactDOM.render(App(), container)
  }
}