package janstenpickle.vault.manage

import janstenpickle.vault.core.VaultSpec
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck

class UserPassIT extends VaultSpec with ScalaCheck {
  import UserPassIT._
  import VaultSpec._

  def is =
    s2"""
      Can create, update and delete a user $good
      Cannot create a user for a non-existent client $badClient
      Cannot create user with a bad policy $badPolicy
    """

  lazy val underTest = UserPass(config)
  lazy val authAdmin = Auth(config)

  def good = Prop.forAllNoShrink(longerStrGen, longerStrGen, longerStrGen, Gen.posNum[Int], longerStrGen, policyGen)(
    (username, password, newPassword, ttl, client, policy) ⇒
      (authAdmin.enable("userpass", Some(client)).attemptRun(_.getMessage()) must beOk) and
      (underTest.create(username, password, ttl, None, client).attemptRun(_.getMessage()) must beOk) and
      (underTest.setPassword(username, newPassword, client).attemptRun(_.getMessage()) must beOk) and
      (underTest.setPolicies(username, policy, client).attemptRun(_.getMessage()) must beOk) and
      (underTest.delete(username, client).attemptRun(_.getMessage()) must beOk) and
      (authAdmin.disable(client).attemptRun(_.getMessage()) must beOk)
  )

  def badClient = Prop.forAllNoShrink(longerStrGen, longerStrGen, Gen.posNum[Int], longerStrGen)(
    (username, password, ttl, client) ⇒
      underTest.create(username, password, ttl, None, client).attemptRun(_.getMessage()) must beFail
  )

  def badPolicy = Prop.forAllNoShrink(longerStrGen,
                                      longerStrGen,
                                      Gen.posNum[Int],
                                      longerStrGen,
                                      Gen.listOf(longerStrGen.suchThat(!policies.contains(_))))(
    (username, password, ttl, client, policy) ⇒
      (authAdmin.enable("userpass", Some(client)).attemptRun(_.getMessage()) must beOk) and
      (underTest.create(username, password, ttl, Some(policy), client).attemptRun(_.getMessage()) must beOk) and
      (authAdmin.disable(client).attemptRun(_.getMessage()) must beOk)
  )
}

object UserPassIT {
  val policies = List("default", "root")
  val policyGen = Gen.listOf(Gen.oneOf(policies))
}