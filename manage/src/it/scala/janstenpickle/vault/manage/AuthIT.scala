package janstenpickle.vault.manage

import janstenpickle.vault.core.VaultSpec
import org.scalacheck.{Prop, Gen}
import org.specs2.ScalaCheck

class AuthIT extends VaultSpec with ScalaCheck {
  import AuthIT._
  import VaultSpec._

  def is =
    s2"""
      Can enable and disable valid auth mount $happy
      Cannot enable an invalid auth type $enableFail
    """

  lazy val underTest = new Auth(config)

  def happy = Prop.forAllNoShrink(
    backends, longerStrGen, Gen.option(longerStrGen))((backend, mount, desc) =>
      (underTest.enable(backend, Some(mount), desc)
      .attemptRun(_.getMessage()) must beOk) and
      (underTest.disable(mount).attemptRun(_.getMessage()) must beOk)
  )

  def enableFail = Prop.forAllNoShrink(
    longerStrGen.suchThat(!backendNames.contains(_)),
    longerStrGen,
    Gen.option(longerStrGen))((backend, mount, desc) =>
      underTest.enable(mount).attemptRun(_.getMessage()) must beFail
  )

}

object AuthIT {
  val backendNames = List("github", "app-id", "ldap", "userpass")
  val backends = Gen.oneOf(backendNames)
}
