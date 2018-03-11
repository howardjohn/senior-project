package io.github.howardjohn.core.impl

import java.security.MessageDigest

import org.scalactic.TolerantNumerics
import org.scalatest._

class DynamoConfigTagSpec extends FlatSpec {
  import DynamoConfigTag._
  "getVersion" should "select the right version" in {
    val versions = Seq(
      DynamoTagEntryVersion(1, "a")
    )
    assert(getVersion(versions, Array(32)) == Some("a"))
  }

  it should "not select a version if all weights are zero" in {
    val versions = Seq(
      DynamoTagEntryVersion(0, "")
    )
    assert(getVersion(versions, Array(32)) == None)
  }

  it should "not select a version if there are none" in {
    val versions = Seq()
    assert(getVersion(versions, Array(32)) == None)
  }

  it should "follow weights accurately" in {
    implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(.01)
    val versions = Seq(
      DynamoTagEntryVersion(9, "a"),
      DynamoTagEntryVersion(1, "b")
    )
    val total = 10000
    val selected = (1 to total)
      .map(i => BigInt(i).toByteArray)
      .map(getVersion(versions, _))
      .collect {
        case Some(v) => v
      }
    val (as, bs) = selected.partition(_ == "a")
    assert(selected.length == total)
    assert(as.length.toDouble/total === .9)
    assert(bs.length.toDouble/total === .1)
  }
}
