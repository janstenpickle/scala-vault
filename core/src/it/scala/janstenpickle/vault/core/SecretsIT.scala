package janstenpickle.vault.core

import org.scalacheck.Prop
import org.specs2.ScalaCheck

class GenericIT extends SecretsTests {
  override def backend: String = "secret"
}

class CubbyHoleIT extends SecretsTests {
  override def backend: String = "cubbyhole"
}

trait SecretsTests extends VaultSpec with ScalaCheck {
  import VaultSpec._

  override def is =
    s2"""
      Can set a secret in vault $set
      Can set and get a secret in vault $get
      Can list keys $list
      Can set multiple subkeys $setMulti
      Can set and get multiple subKeys $getSetMulti
      Cannot get non-existent key $failGet
      Fails to perform actions on a non-vault server $failSetBadServer
      Fails to perform actions with a bad token $failSetBadToken
      """

  def backend: String

  lazy val good = Secrets(config, backend)
  lazy val badToken = Secrets(badTokenConfig, backend)
  lazy val badServer = Secrets(badServerConfig, backend)

  def set = Prop.forAllNoShrink(strGen, strGen) { (key, value) =>
    good.set(key, value).attemptRun(_.getMessage()) must beOk
  }

  def get = Prop.forAllNoShrink(strGen, strGen) { (key, value) =>
    (good.set(key, value).attemptRun(_.getMessage()) must beOk) and
    (good.get(key).attemptRun(_.getMessage()) must beOk.like {
      case a => a === value
    })
  }

  def list = Prop.forAllNoShrink(strGen, strGen, strGen) { (key1, key2, value) =>
    (good.set(key1, value).attemptRun(_.getMessage()) must beOk) and
    (good.set(key2, value).attemptRun(_.getMessage()) must beOk) and
    (good.list.attemptRun(_.getMessage()) must beOk[List[String]].like {
      case a => a must containAllOf(Seq(key1, key2))
    })
  }

  def setMulti = Prop.forAllNoShrink(strGen, strGen, strGen, strGen) {
    (key1, key2, value1, value2) =>
    good.set(
      "nicolas-cage",
      Map(key1 -> value1, key2 -> value2)
    ).attemptRun(_.getMessage()) must beOk
  }

  def getSetMulti = Prop.forAllNoShrink(
    strGen, strGen, strGen, strGen, strGen
  ) { (key1, key2, value1, value2, mainKey) =>
    val testData = Map(key1 -> value1, key2 -> value2)
    (good.set(mainKey, testData).attemptRun(_.getMessage()) must beOk) and
    (good.getAll(mainKey).attemptRun(_.getMessage()) must beOk.like {
      case a => a === testData
    })
  }

  def failGet = good.get("john").attemptRun(_.getMessage()) must beFail
    .like { case err =>
      err must contain("Received failure response from server: 404")
    }

  def failSetBadServer = badServer.set(
    "nic", "cage"
  ).attemptRun(_.getMessage()) must beFail

  def failSetBadToken = badToken.set(
    "nic", "cage"
  ).attemptRun(_.getMessage()) must beFail
}
