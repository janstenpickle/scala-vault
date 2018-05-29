package janstenpickle.vault.manage

import janstenpickle.vault.core.VaultSpec
import janstenpickle.vault.manage.Model.Rule
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck

class PolicyIT extends VaultSpec with ScalaCheck {
  import PolicyIT._
  import VaultSpec._

  override def is =
    s2"""
      Can successfully set and get policies $happy
      Cannot set an invalid policy $sad
    """

  lazy val underTest = Policy(config)

  def happy = Prop.forAllNoShrink(
    longerStrGen,
    Gen.listOf(ruleGen(longerStrGen, policyGen, capabilitiesGen)).
    suchThat(_.nonEmpty)) { (name, rules) =>
      (underTest.set(name.toLowerCase, rules)
      .attemptRun(_.getMessage()) must beOk) and
      (underTest.inspect(name.toLowerCase)
      .attemptRun(_.getMessage()) must beOk) and
      (underTest.delete(name.toLowerCase).attemptRun(_.getMessage()) must beOk)
  }

  // cannot use generated values here as
  // vault seems to have a failure rate limit
  def sad = underTest.set(
    "nic", List(Rule("cage", Some(List("kim", "copolla"))))
  ).attemptRun(_.getMessage()) must beFail
}

object PolicyIT {
  val policyGen = Gen.option(Gen.oneOf("read", "write", "sudo", "deny"))
  val capabilitiesGen =
    Gen.listOf(Gen.oneOf(
      "create", "read", "update", "delete", "list", "sudo", "deny")).
      suchThat(_.nonEmpty).
      map(_.distinct)

  def ruleGen(
    pathGen: Gen[String],
    polGen: Gen[Option[String]],
    capGen: Gen[List[String]]
  ) = for {
    path <- pathGen
    policy <- polGen
    capabilities <- capGen
  } yield Rule(path, Some(capabilities), policy)
}

