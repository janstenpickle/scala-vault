package janstenpickle.vault.auth

import java.net.{HttpURLConnection => status}

import io.circe.generic.auto._
import janstenpickle.scala.syntax.AsyncResultSyntax._
import janstenpickle.scala.syntax.ResponseSyntax._
import janstenpickle.scala.syntax.SyntaxRequest._
import janstenpickle.vault.core.WSClient
import uscala.concurrent.result.AsyncResult

import scala.concurrent.ExecutionContext

case class UserPass(wsClient: WSClient) {

  def authenticate(
    username: String,
    password: String,
    ttl: Int,
    client: String = "userpass"
  )(implicit ec: ExecutionContext): AsyncResult[String, UserPassResponse] =
    wsClient.path(s"auth/$client/login/$username")
      .post(Map("password" -> password, "ttl" -> s"${ttl}s"))
      .toAsyncResult
      .acceptStatusCodes(status.HTTP_OK)
      .extractFromJson[UserPassResponse](_.downField("auth"))
}

case class UserPassResponse(
  client_token: String,
  policies: List[String],
  metadata: Option[Map[String, String]],
  lease_duration: Int,
  renewable: Boolean
)
