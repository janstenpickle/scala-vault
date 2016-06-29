package janstenpickle.vault.manage

import com.ning.http.client.Response
import janstenpickle.concurrent.result.AsyncResult
import janstenpickle.scala.syntax.option._
import janstenpickle.scala.syntax.request._
import janstenpickle.scala.syntax.response._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.vault.core.VaultConfig

import scala.concurrent.ExecutionContext

case class UserPass(config: VaultConfig) {
  final val DefaultClient = "userpass"
  def create(username: String,
             password: String,
             ttl: Int,
             policies: Option[List[String]] = None,
             client: String = DefaultClient)
            (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username")(
      _.post(policies.map(_.mkString(",")).toMap("policies") ++
                         Map("username" -> username,
                             "password" -> password,
                             "ttl" -> s"${ttl}s"))
    ).execute.acceptStatusCodes(204)

  def delete(username: String, client: String = DefaultClient)(implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username")(_.delete).
      execute.
      acceptStatusCodes(204)

  def setPassword(username: String, password: String, client: String = DefaultClient)
                 (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username/password")(
      _.post(Map("username" -> username, "password" -> password))
    ).execute.acceptStatusCodes(204)

  def setPolicies(username: String, policies: List[String], client: String = DefaultClient)
                 (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username/policies")(
      _.post(Map("username" -> username, "policies" -> policies.mkString(",")))
    ).execute.acceptStatusCodes(204)
}
