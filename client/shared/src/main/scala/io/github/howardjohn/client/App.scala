package io.github.howardjohn.client

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("App")
class App {
  @JSExport
  def get(): String = "Hello JS!"
}