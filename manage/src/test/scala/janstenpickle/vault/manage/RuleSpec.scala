package janstenpickle.vault.manage

import janstenpickle.vault.manage.Model.Rule
import org.scalacheck.{Gen, Prop}
import org.specs2.{ScalaCheck, Specification}
import uscala.result.specs2.ResultMatchers

class RuleSpec extends Specification with ScalaCheck with ResultMatchers {
  import RuleSpec._

  override def is =
    s2"""
      Can encode and decode policy strings $passes
      Cannot decode bad policy strings $fails
      """

  def passes = Prop.forAllNoShrink(Gen.listOf(ruleGen).suchThat(_.nonEmpty)) (rules =>
    Rule.decode(rules.map(_.encode).mkString("\n")) must beOk.like {
      case a => a must containAllOf(rules)
    }
  )

  def fails = Prop.forAllNoShrink(Gen.listOf(badRuleGen).suchThat(_.nonEmpty)) (rules =>
    Rule.decode(rules.mkString("\n")) must beFail
  )
}

object RuleSpec {
  val policyGen = Gen.option(Gen.oneOf("read", "write", "sudo", "deny"))
  val capabilitiesGen = Gen.option(
    Gen.listOf(Gen.oneOf("create", "read", "update", "delete", "list", "sudo", "deny")).
      suchThat(_.nonEmpty).
      map(_.distinct)
  )

  val ruleGen = for {
    path <- Gen.alphaStr.suchThat(_.nonEmpty)
    policy <- policyGen
    capabilities <- capabilitiesGen
  } yield Rule(path, capabilities, policy)

  val badRuleGen = for {
    path <- Gen.alphaStr.suchThat(_.nonEmpty)
    policy <- policyGen
    capabilities <- capabilitiesGen
  } yield
    s"""
       |path "$path"
       |   $policy cage
       |   $capabilities }""".stripMargin('|')
}
