package janstenpickle.vault.core

import org.scalacheck.Prop
import org.specs2.ScalaCheck

class SecretsIT extends VaultSpec with ScalaCheck {
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

  lazy val good = Secrets(config)
  lazy val badToken = Secrets(badTokenConfig)
  lazy val badServer = Secrets(badServerConfig)

  def set = Prop.forAllNoShrink(strGen, strGen) { (key, value) =>
    good.set(key, value).unsafePerformSyncAttempt must be_\/-
  }

  def get = Prop.forAllNoShrink(strGen, strGen) { (key, value) =>
    (good.set(key, value).unsafePerformSyncAttempt must be_\/-) and
    (good.get(key).unsafePerformSyncAttempt must be_\/-.like { case a => a === value })
  }

  def list = Prop.forAllNoShrink(strGen, strGen, strGen) { (key1, key2, value) =>
    (good.set(key1, value).unsafePerformSyncAttempt must be_\/-) and
    (good.set(key2, value).unsafePerformSyncAttempt must be_\/-) and
    (good.list.unsafePerformSyncAttempt must be_\/-.like { case a => a must containAllOf(Seq(key1, key2)) })
  }

  def setMulti = Prop.forAllNoShrink(strGen, strGen, strGen, strGen) { (key1, key2, value1, value2) =>
    good.set("nicolas-cage", Map(key1 -> value1, key2 -> value2)).unsafePerformSyncAttempt must be_\/-
  }

  def getSetMulti = Prop.forAllNoShrink(strGen, strGen, strGen, strGen, strGen) { (key1, key2, value1, value2, mainKey) =>
    val testData = Map(key1 -> value1, key2 -> value2)
    (good.set(mainKey, testData).unsafePerformSyncAttempt must be_\/-) and
    (good.getAll(mainKey).unsafePerformSyncAttempt must be_\/-.like { case a => a === testData })
  }

  def failGet = good.get("john").unsafePerformSyncAttempt must be_-\/.
    like { case ex => ex.getMessage must contain("Received failure response from server: 404") }

  def failSetBadServer = badServer.set("nic", "cage").unsafePerformSyncAttempt must be_-\/

  def failSetBadToken = badToken.set("nic", "cage").unsafePerformSyncAttempt must be_-\/
}
