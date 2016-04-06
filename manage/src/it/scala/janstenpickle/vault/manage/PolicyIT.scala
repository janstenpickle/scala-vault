package janstenpickle.vault.manage

import janstenpickle.vault.core.VaultSpec
import janstenpickle.vault.manage.Model.Rule
import org.scalacheck.{Prop, Gen}
import org.specs2.ScalaCheck
import scalaz.\/
import scalaz.syntax.either._

class PolicyIT extends VaultSpec with ScalaCheck {
  import PolicyIT._
  import VaultSpec._

  override def is =
    s2"""
      Can successfully set and get policies $happy
      Cannot set an invalid policy $sad
    """

  lazy val underTest = Policy(config)

  def happy = Prop.forAllNoShrink(longerStrGen,
                                  Gen.listOf(ruleGen(longerStrGen, policyGen, capabilitiesGen)).
                                   suchThat(_.nonEmpty)) { (name, rules) =>
    (underTest.set(name.toLowerCase, rules).unsafePerformSyncAttempt must be_\/-) and
    (underTest.inspect(name.toLowerCase).unsafePerformSyncAttempt.
      flatMap(_.decodeRules.fold[Throwable \/ List[Rule]](new RuntimeException().left)(identity)) must be_\/-.
      like { case a => a must containTheSameElementsAs(rules) }) and
    (underTest.delete(name.toLowerCase).unsafePerformSyncAttempt must be_\/-)
  }

  def sad = underTest.set("nic", List(Rule("cage", Some(List("kim", "copolla"))))).unsafePerformSyncAttempt must be_-\/
}

object PolicyIT {
  val policyGen = Gen.option(Gen.oneOf("read", "write", "sudo", "deny"))
  val capabilitiesGen =
    Gen.listOf(Gen.oneOf("create", "read", "update", "delete", "list", "sudo", "deny")).
      suchThat(_.nonEmpty).
      map(_.distinct)

  def ruleGen(pathGen: Gen[String], polGen: Gen[Option[String]], capGen: Gen[List[String]]) = for {
    path <- pathGen
    policy <- polGen
    capabilities <- capGen
  } yield Rule(path, Some(capabilities), policy)
}

