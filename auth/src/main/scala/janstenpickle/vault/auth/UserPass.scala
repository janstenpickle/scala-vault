package janstenpickle.vault.auth

import janstenpickle.vault.core.WSClientWrapper
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.scala.syntax.task._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

case class UserPass(wsClient: WSClientWrapper) {
  implicit val userPassResponseFormat = Json.format[UserPassResponse]

  def authenticate(username: String, password: String, ttl: Int, client: String = "userpass")
                  (implicit ec: ExecutionContext): Task[UserPassResponse] =
    wsClient.path(s"auth/$client/login/$username").
      post(Json.toJson(Map("password" -> password, "ttl" -> s"${ttl}s"))).
      toTask.
      acceptStatusCodes(200).
      extractFromJson[UserPassResponse](_ \ "auth")
}

case class UserPassResponse(client_token: String,
                            policies: List[String],
                            metadata: Option[Map[String, String]],
                            lease_duration: Int,
                            renewable: Boolean)
