package janstenpickle.vault.auth

import janstenpickle.vault.core.WSClient
import janstenpickle.scala.syntax.response._
import janstenpickle.scala.syntax.request._
import janstenpickle.scala.syntax.task._

import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

case class UserPass(wsClient: WSClient) {


  def authenticate(username: String, password: String, ttl: Int, client: String = "userpass")
                  (implicit ec: ExecutionContext): Task[UserPassResponse] =
    wsClient.path(s"auth/$client/login/$username").
      post(Map("password" -> password, "ttl" -> s"${ttl}s")).
      toTask.
      acceptStatusCodes(200).
      extractFromJson[UserPassResponse](_.downField("auth"))
}

case class UserPassResponse(client_token: String,
                            policies: List[String],
                            metadata: Option[Map[String, String]],
                            lease_duration: Int,
                            renewable: Boolean)
