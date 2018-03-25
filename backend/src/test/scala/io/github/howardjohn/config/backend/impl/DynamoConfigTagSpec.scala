package io.github.howardjohn.config.backend.impl

import org.scalactic.TolerantNumerics
import org.scalatest._

class DynamoConfigTagSpec extends FlatSpec {
  import DynamoConfigTag._
  "getVersion" should "select the right version" in {
    val versions = Map(
      "a" -> 1
    )
    assert(getVersion(versions, Array(32)) == Some("a"))
  }

  it should "not select a version if all weights are zero" in {
    val versions = Map(
      "a" -> 0
    )
    assert(getVersion(versions, Array(32)) == None)
  }

  it should "not select a version if there are none" in {
    val versions = Map[String, Int]()
    assert(getVersion(versions, Array(32)) == None)
  }

  it should "follow weights accurately" in {
    implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(.01)
    val versions = Map("a" -> 9, "b" -> 1)
    val total = 10000
    val selected = (1 to total)
      .map(i => BigInt(i).toByteArray)
      .map(getVersion(versions, _))
      .collect {
        case Some(v) => v
      }
    val (as, bs) = selected.partition(_ == "a")
    assert(selected.length == total)
    assert(as.length.toDouble / total === .9)
    assert(bs.length.toDouble / total === .1)
  }

  it should "follow weights accurately using md5" in {
    implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(.01)
    val versions = Map("a" -> 1, "b" -> 1)
    val total = 10000
    val selected = (1 to total)
      .map(_.toString)
      .map(DynamoConfigTag.md5)
      .map(getVersion(versions, _))
      .collect {
        case Some(v) => v
      }
    val (as, bs) = selected.partition(_ == "a")
    assert(selected.length == total)
    assert(as.length.toDouble / total === bs.length.toDouble / total)
  }
}
