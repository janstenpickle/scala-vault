package janstenpickle.vault.manage

import com.ning.http.client.Response
import io.circe.generic.auto._
import io.circe.syntax._
import janstenpickle.scala.syntax.OptionSyntax._
import janstenpickle.scala.syntax.SyntaxRequest._
import janstenpickle.scala.syntax.ResponseSyntax._
import janstenpickle.scala.syntax.VaultConfigSyntax._
import janstenpickle.vault.core.VaultConfig
import janstenpickle.vault.manage.Model._
import uscala.concurrent.result.AsyncResult
import uscala.result.Result

import scala.concurrent.ExecutionContext

// scalastyle:off magic.number
case class Auth(config: VaultConfig) {
  def enable(`type`: String,
             mountPoint: Option[String] = None,
             description: Option[String] = None)
            (implicit ec: ExecutionContext): AsyncResult[String, Response] =
     config.authenticatedRequest(s"sys/auth/${mountPoint.getOrElse(`type`)}")(
       _.post(description.toMap("description") + ("type" -> `type`))
     ).execute.acceptStatusCodes(204)

  def disable(mountPoint: String)
  (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"sys/auth/$mountPoint")(_.delete).
      execute.
      acceptStatusCodes(204)
}

case class Mounts(config: VaultConfig) {
  def remount(from: String, to: String)
  (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest("sys/remount")(
      _.post(Map("from" -> from, "to" -> to))
    ).execute.acceptStatusCodes(204)

  def list(implicit ec: ExecutionContext):
  AsyncResult[String, Map[String, Mount]] =
    config.authenticatedRequest("sys/mounts")(_.get).
      execute.
      acceptStatusCodes(200).
      extractFromJson[Map[String, Mount]](_.downField("data"))

  def mount(`type`: String,
            mountPoint: Option[String] = None,
            description: Option[String] = None,
            conf: Option[Mount] = None)
           (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"sys/mounts/${mountPoint.getOrElse(`type`)}")(
      _.post(MountRequest(`type`, description, conf).asJson)
    ).execute.acceptStatusCodes(204)

  def delete(mountPoint: String)
  (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"sys/mounts/$mountPoint")(_.delete).
      execute.
      acceptStatusCodes(204)
}

case class Policy(config: VaultConfig) {

  def list(implicit ec: ExecutionContext): AsyncResult[String, List[String]] =
    config.authenticatedRequest("sys/policy")(_.get).
      execute.
      acceptStatusCodes(200).
      extractFromJson[List[String]](_.downField("policies"))

  // NOTE: `rules` is not valid Json
  def inspect(policy: String)(implicit ec: ExecutionContext):
  AsyncResult[String, String] =
    config.authenticatedRequest(s"sys/policy/$policy")(_.get).
      execute.
      acceptStatusCodes(200)
      .extractFromJson[String](_.downField("rules"))

  def set(policy: String, rules: List[Rule])
  (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"sys/policy/$policy")(
      _.post(PolicySetting(policy, rules).asJson)).
      execute.
      acceptStatusCodes(204)

  def delete(policy: String)(implicit ec: ExecutionContext):
  AsyncResult[String, Response] =
    config.authenticatedRequest(s"sys/policy/$policy")(_.delete).
      execute.
      acceptStatusCodes(204)
}

object Model {
  case class MountRequest(`type`: String,
                          description: Option[String],
                          config: Option[Mount])
  case class Mount(`type`: String,
                   description: Option[String],
                   config: Option[MountConfig])
  case class MountConfig(
    default_lease_ttl: Option[Int],
    max_lease_ttl: Option[Int],
    force_no_cache: Option[Boolean]
  )

  case class PolicySetting(name: String, rules: Option[String]) {
    lazy val decodeRules: Option[Result[String, List[Rule]]] = rules.filter(
      _.nonEmpty).map(Rule.decode)
  }
  object PolicySetting {
    def apply(name: String, rules: List[Rule]): PolicySetting =
      PolicySetting(name, Option(rules.map(_.encode).mkString("\n")))
  }
  case class Rule(
    path: String,
    capabilities: Option[List[String]] = None,
    policy: Option[String] = None
  ) {
    lazy val encodeCapabilities = capabilities.filter(_.nonEmpty).map(caps =>
      s"capabilities = [${caps.map(c => s""""$c"""").mkString(", ")}]"
    ).getOrElse("")

    lazy val encodePolicy = policy.map(pol =>
      s"""policy = "$pol""""
    ).getOrElse("")

    lazy val encode =
      s"""
      |path "$path" {
      |   $encodePolicy
      |   $encodeCapabilities
      |}""".stripMargin('|')
  }
  object Rule {
    val pathRegex = """\s*path\s+"(\S+)"\s+\{""".r
    val capabilitiesRegex = """\s+capabilities\s+=\s+\[(.+)\]""".r
    val policyRegex = """\s+policy\s+=\s+"(\S+)"""".r

    def decode(ruleString: String): Result[String, List[Rule]] = {
      val rules = ruleString.split("""\s*}\s+\n""").toList
      val decoded = rules.foldLeft(List.empty[Rule])( (acc, v) =>
        acc ++ pathRegex.findFirstMatchIn(v).map(_.group(1)).map(path =>
          Rule(
            path,
            capabilitiesRegex.findFirstMatchIn(v).map(_.group(1).split(',')
              .map(_.trim.replace("\"", "")).toList),
            policyRegex.findFirstMatchIn(v).map(_.group(1))
          )
        )
      )
      if (decoded.isEmpty) {
        Result.fail(s"Could not find any valid rules in string: $ruleString")
      }
      else {
        Result.ok(decoded)
      }
    }
  }
}
// scalastyle:on magic.number
