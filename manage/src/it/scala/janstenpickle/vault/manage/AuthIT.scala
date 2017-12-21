package janstenpickle.vault.manage

import janstenpickle.scala.syntax.AsyncResultSyntax._
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
      .attemptRun must beRight) and
      (underTest.disable(mount).attemptRun must beRight)
  )

  def enableFail = Prop.forAllNoShrink(
    longerStrGen.suchThat(!backendNames.contains(_)),
    longerStrGen,
    Gen.option(longerStrGen))((backend, mount, desc) =>
      underTest.enable(mount).attemptRun must beLeft
  )

}

object AuthIT {
  val backendNames = List("github", "app-id", "ldap", "userpass")
  val backends = Gen.oneOf(backendNames)
}
