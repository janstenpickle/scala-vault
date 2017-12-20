package janstenpickle.vault.manage

import java.net.{HttpURLConnection => status}

import janstenpickle.scala.syntax.OptionSyntax._
import janstenpickle.scala.syntax.ResponseSyntax._
import janstenpickle.scala.syntax.SyntaxRequest._
import janstenpickle.scala.syntax.VaultConfigSyntax._
import janstenpickle.vault.core.VaultConfig
import org.asynchttpclient.Response
import uscala.concurrent.result.AsyncResult

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
    ).execute.acceptStatusCodes(status.HTTP_NO_CONTENT)

  def delete(username: String, client: String = DefaultClient)
  (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username")(_.delete)
      .execute
      .acceptStatusCodes(status.HTTP_NO_CONTENT)

  def setPassword(
    username: String,
    password: String,
    client: String = DefaultClient
  )(implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username/password")(
      _.post(Map("username" -> username, "password" -> password))
    ).execute.acceptStatusCodes(status.HTTP_NO_CONTENT)

  def setPolicies(
    username: String,
    policies: List[String],
    client: String = DefaultClient
  )(implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username/policies")(
      _.post(Map("username" -> username, "policies" -> policies.mkString(",")))
    ).execute.acceptStatusCodes(status.HTTP_NO_CONTENT)
}
