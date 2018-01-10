package janstenpickle.vault.manage

import janstenpickle.vault.core.VaultSpec
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import janstenpickle.scala.syntax.AsyncResultSyntax._
import org.specs2.matcher.EitherMatchers

class UserPassIT extends VaultSpec with ScalaCheck with EitherMatchers {
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
    (username, password, newPassword, ttl, client, policy) =>
      (authAdmin.enable("userpass", Some(client)).attemptRun must beRight) and
      (underTest.create(username, password, ttl, None, client).attemptRun must beRight) and
      (underTest.setPassword(username, newPassword, client).attemptRun must beRight) and
      (underTest.setPolicies(username, policy, client).attemptRun must beRight) and
      (underTest.delete(username, client).attemptRun must beRight) and
      (authAdmin.disable(client).attemptRun must beRight)
  )

  def badClient = Prop.forAllNoShrink(longerStrGen, longerStrGen, Gen.posNum[Int], longerStrGen)(
    (username, password, ttl, client) =>
      underTest.create(username, password, ttl, None, client).attemptRun must beLeft
  )

  def badPolicy = Prop.forAllNoShrink(longerStrGen,
                                      longerStrGen,
                                      Gen.posNum[Int],
                                      longerStrGen,
                                      Gen.listOf(longerStrGen.suchThat(!policies.contains(_))))(
    (username, password, ttl, client, policy) =>
      (authAdmin.enable("userpass", Some(client)).attemptRun must beRight) and
      (underTest.create(username, password, ttl, Some(policy), client).attemptRun must beRight) and
      (authAdmin.disable(client).attemptRun must beRight)
  )
}

object UserPassIT {
  val policies = List("default", "root")
  val policyGen = Gen.listOf(Gen.oneOf(policies))
}