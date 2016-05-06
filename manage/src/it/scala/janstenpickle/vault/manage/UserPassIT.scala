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
      Cannont create user with a bad policy $badPolicy
    """

  lazy val underTest = UserPass(config)
  lazy val authAdmin = Auth(config)

  def good = Prop.forAllNoShrink(longerStrGen, longerStrGen, longerStrGen, Gen.posNum[Int], longerStrGen, policyGen)(
    (username, password, newPassword, ttl, client, policy) =>
      (authAdmin.enable("userpass", Some(client)).unsafePerformSyncAttempt must be_\/-) and
      (underTest.create(username, password, ttl, None, client).unsafePerformSyncAttempt must be_\/-) and
      (underTest.setPassword(username, newPassword, client).unsafePerformSyncAttempt must be_\/-) and
      (underTest.setPolicies(username, policy, client).unsafePerformSyncAttempt must be_\/-) and
      (underTest.delete(username, client).unsafePerformSyncAttempt must be_\/-) and
      (authAdmin.disable(client).unsafePerformSyncAttempt must be_\/-)
  )

  def badClient = Prop.forAllNoShrink(longerStrGen, longerStrGen, Gen.posNum[Int], longerStrGen)(
    (username, password, ttl, client) =>
      underTest.create(username, password, ttl, None, client).unsafePerformSyncAttempt must be_-\/
  )

  def badPolicy = Prop.forAllNoShrink(longerStrGen,
                                      longerStrGen,
                                      Gen.posNum[Int],
                                      longerStrGen,
                                      Gen.listOf(longerStrGen.suchThat(!policies.contains(_))))(
    (username, password, ttl, client, policy) =>
      (authAdmin.enable("userpass", Some(client)).unsafePerformSyncAttempt must be_\/-) and
      (underTest.create(username, password, ttl, Some(policy), client).unsafePerformSyncAttempt must be_\/-) and
      (authAdmin.disable(client).unsafePerformSyncAttempt must be_\/-)
  )
}

object UserPassIT {
  val policies = List("default", "root")
  val policyGen = Gen.listOf(Gen.oneOf(policies))
}