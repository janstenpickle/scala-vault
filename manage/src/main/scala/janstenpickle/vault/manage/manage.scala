package janstenpickle.vault.manage

import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.option._
import janstenpickle.vault.core.VaultConfig
import janstenpickle.vault.manage.Model._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

case class Auth(config: VaultConfig) {
  def enable(`type`: String,
             mountPoint: Option[String] = None,
             description: Option[String] = None)
            (implicit ec: ExecutionContext): Task[WSResponse] =
     config.authenticatedRequest(s"sys/auth/${mountPoint.getOrElse(`type`)}")(
       _.post(Json.toJson(description.toMap("description") + ("type" -> `type`)))
     ).acceptStatusCodes(204)

  def disable(mountPoint: String)(implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"sys/auth/$mountPoint")(_.delete()).acceptStatusCodes(204)
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
    config.authenticatedRequest("sys/mounts")(_.get())
      .acceptStatusCodes(200).extractFromJson[Map[String, Mount]]()

  def mount(`type`: String,
            mountPoint: Option[String] = None,
            description: Option[String] = None,
            conf: Option[Mount] = None)
           (implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"sys/mounts/${mountPoint.getOrElse(`type`)}")(
      _.post(Json.toJson(MountRequest(`type`, description, conf)))
    ).acceptStatusCodes(204)

  def delete(mountPoint: String)(implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"sys/mounts/$mountPoint")(_.delete()).acceptStatusCodes(204)
}

case class Policy(config: VaultConfig) {
  def list(implicit ec: ExecutionContext): Task[List[String]] =
    config.authenticatedRequest("sys/policy")(_.get())
      .acceptStatusCodes(200).extractFromJson[List[String]](_ \ "policies")

  def inspect(policy: String)(implicit ec: ExecutionContext): Task[JsValue] =
    config.authenticatedRequest(s"sys/policy/$policy")(_.get())
      .acceptStatusCodes(200).extractJson
}

object Model {
  case class MountRequest(`type`: String,
                          description: Option[String],
                          config: Option[Mount])
  case class Mount(`type`: String,
                   description: Option[String],
                   config: Option[MountConfig])
  case class MountConfig(default_lease_ttl: Int, max_lease_ttl: Int)
}