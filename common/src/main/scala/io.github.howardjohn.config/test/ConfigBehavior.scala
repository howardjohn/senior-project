package io.github.howardjohn.config.test

import io.github.howardjohn.config.{ConfigDatastore, Result}
import org.scalatest.Matchers._
import org.scalatest.{FeatureSpec, GivenWhenThen}

trait ConfigBehavior { this: FeatureSpec with GivenWhenThen =>
  import ConfigBehavior._

  def configDatastore[B[_]](ds: => ConfigDatastore[B])(implicit ev: B[String]) = {
    scenario("a tag is created") {
      Given("a namespace with no tags")
      val ns = ds.getNamespace[String](namespace)
      assert(runIO(ns.getTag(tag).getDetails()).isEmpty)

      When("we create a new tag")
      val details = runIO(for {
        tag <- ns.createTag(tag)
        _ <- tag.moveTag(Map(version -> 1))
        details <- tag.getDetails()
      } yield details)

      Then("the tag should point to a version")
      assert(details.map(_.version) === Some(version))
    }

    scenario("a version is created") {
      Given("a namespace with no versions")
      val ns = ds.getNamespace[String](namespace)
      runIO(ns.getVersions()) shouldBe empty

      When("we create a new version")
      val details = runIO(for {
        version <- ns.createVersion(version)
        details <- version.details()
      } yield details)

      Then("the version should be created")
      assert(details.map(_.version) === Some(version))

      And("it should initially be unfrozen")
      assert(details.map(_.frozen) === Some(false))
    }
  }

  private def runIO[T](r: Result[T]): T =
    r.value
      .unsafeRunSync()
      .fold(
        err => fail(s"Test encountered error: $err"),
        identity
      )
}

object ConfigBehavior {
  val namespace = "namespace"
  val tag = "tag"
  val version = "version"
}
