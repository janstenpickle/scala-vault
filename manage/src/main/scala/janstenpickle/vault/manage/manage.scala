package janstenpickle.vault.manage

import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.option._
import janstenpickle.vault.core.VaultConfig
import janstenpickle.vault.manage.Model._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext
import scalaz.\/
import scalaz.concurrent.Task
import scalaz.syntax.either._

case class Auth(config: VaultConfig) {
  def enable(`type`: String,
             mountPoint: Option[String] = None,
             description: Option[String] = None)
            (implicit ec: ExecutionContext): Task[WSResponse] =
     config.authenticatedRequest(s"sys/auth/${mountPoint.getOrElse(`type`)}")(
       _.post(Json.toJson(description.toMap("description") + ("type" -> `type`)))
     ).acceptStatusCodes(204)

  def disable(mountPoint: String)(implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"sys/auth/$mountPoint")(_.delete()).
      acceptStatusCodes(204)
}

case class Mounts(config: VaultConfig) {
  implicit val mountCountFormat = Json.format[MountConfig]
  implicit val mountFormat = Json.format[Mount]
  implicit val mountRequestFormat = Json.format[MountRequest]

  def remount(from: String, to: String)(implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest("sys/remount")(
      _.post(Json.toJson(Map("from" -> from, "to" -> to)))
    ).acceptStatusCodes(204)

  def list(implicit ec: ExecutionContext): Task[Map[String, Mount]] =
    config.authenticatedRequest("sys/mounts")(_.get()).
      acceptStatusCodes(200).extractFromJson[Map[String, Mount]]()

  def mount(`type`: String,
            mountPoint: Option[String] = None,
            description: Option[String] = None,
            conf: Option[Mount] = None)
           (implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"sys/mounts/${mountPoint.getOrElse(`type`)}")(
      _.post(Json.toJson(MountRequest(`type`, description, conf)))
    ).acceptStatusCodes(204)

  def delete(mountPoint: String)(implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"sys/mounts/$mountPoint")(_.delete()).
      acceptStatusCodes(204)
}

case class Policy(config: VaultConfig) {
  implicit val policySettingFormat = Json.format[PolicySetting]

  def list(implicit ec: ExecutionContext): Task[List[String]] =
    config.authenticatedRequest("sys/policy")(_.get()).
      acceptStatusCodes(200).extractFromJson[List[String]](_ \ "policies")

  def inspect(policy: String)(implicit ec: ExecutionContext): Task[PolicySetting] =
    config.authenticatedRequest(s"sys/policy/$policy")(_.get()).
      acceptStatusCodes(200).extractFromJson[PolicySetting]()

  def set(policy: String, rules: List[Rule])(implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"sys/policy/$policy")(_.post(Json.toJson(PolicySetting(policy, rules)))).
      acceptStatusCodes(204)

  def delete(policy: String)(implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"sys/policy/$policy")(_.delete()).
      acceptStatusCodes(204)
}

object Model {
  case class MountRequest(`type`: String,
                          description: Option[String],
                          config: Option[Mount])
  case class Mount(`type`: String,
                   description: Option[String],
                   config: Option[MountConfig])
  case class MountConfig(default_lease_ttl: Int, max_lease_ttl: Int)

  case class PolicySetting(name: String, rules: Option[String]) {
    lazy val decodeRules: Option[Throwable \/ List[Rule]] = rules.filter(_.nonEmpty).map(Rule.decode)
  }
  object PolicySetting {
    def apply(name: String, rules: List[Rule]): PolicySetting =
      PolicySetting(name, Option(rules.map(_.encode).mkString("\n")))
  }
  case class Rule(path: String, capabilities: Option[List[String]] = None, policy: Option[String] = None) {
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

    def decode(ruleString: String): Throwable \/ List[Rule] = {
      val rules = ruleString.split("""\s*}\s+\n""").toList
      val decoded = rules.foldLeft(List.empty[Rule])( (acc, v) =>
        acc ++ pathRegex.findFirstMatchIn(v).map(_.group(1)).map(path =>
          Rule(path,
               capabilitiesRegex.findFirstMatchIn(v).map(_.group(1).split(',').map(_.trim.replace("\"", "")).toList),
               policyRegex.findFirstMatchIn(v).map(_.group(1)))
        )
      )
      if (decoded.isEmpty) new RuntimeException(s"Could not find any valid rules in string: $ruleString").left
      else decoded.right
    }
  }
}