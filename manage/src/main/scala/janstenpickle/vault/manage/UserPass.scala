package janstenpickle.vault.manage

import com.ning.http.client.Response
import janstenpickle.scala.syntax.CatsOption._
import janstenpickle.scala.syntax.CatsRequest._
import janstenpickle.scala.syntax.CatsResponse._
import janstenpickle.scala.syntax.CatsVaultConfig._
import janstenpickle.vault.core.VaultConfig
import uscala.concurrent.result.AsyncResult

import scala.concurrent.ExecutionContext

// scalastyle:off magic.number
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

  def delete(username: String, client: String = DefaultClient)
  (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username")(_.delete).
      execute.
      acceptStatusCodes(204)

  def setPassword(
    username: String,
    password: String,
    client: String = DefaultClient
  )(implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username/password")(
      _.post(Map("username" -> username, "password" -> password))
    ).execute.acceptStatusCodes(204)

  def setPolicies(
    username: String,
    policies: List[String],
    client: String = DefaultClient
  )(implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(s"auth/$client/users/$username/policies")(
      _.post(Map("username" -> username, "policies" -> policies.mkString(",")))
    ).execute.acceptStatusCodes(204)
}
// scalastyle:on magic.number
