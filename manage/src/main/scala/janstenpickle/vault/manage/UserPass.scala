package janstenpickle.vault.manage

import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.option._
import janstenpickle.vault.core.VaultConfig
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

case class UserPass(config: VaultConfig) {
  final val DefaultClient = "userpass"
  def create(username: String,
             password: String,
             ttl: Int,
             policies: Option[List[String]] = None,
             client: String = DefaultClient)
            (implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"auth/$client/users/$username")(
      _.post(Json.toJson(policies.map(_.mkString(",")).toMap("policies") ++
                         Map("username" -> username,
                             "password" -> password,
                             "ttl" -> s"${ttl}s")))
    ).acceptStatusCodes(204)

  def delete(username: String, client: String = DefaultClient)(implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"auth/$client/users/$username")(_.delete()).acceptStatusCodes(204)

  def setPassword(username: String, password: String, client: String = DefaultClient)
                 (implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"auth/$client/users/$username/password")(
      _.post(Json.toJson(Map("username" -> username, "password" -> password)))
    ).acceptStatusCodes(204)

  def setPolicies(username: String, policies: List[String], client: String = DefaultClient)
                 (implicit ec: ExecutionContext): Task[WSResponse] =
    config.authenticatedRequest(s"auth/$client/users/$username/policies")(
      _.post(Json.toJson(Map("username" -> username, "policies" -> policies.mkString(","))))
    ).acceptStatusCodes(204)
}
