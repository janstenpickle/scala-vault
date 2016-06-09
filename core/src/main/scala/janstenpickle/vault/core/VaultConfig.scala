package janstenpickle.vault.core

import java.net.URL

import dispatch.{Req, url}
import io.circe.generic.auto._
import io.circe.syntax._
import janstenpickle.scala.syntax.request._
import janstenpickle.scala.syntax.response._
import janstenpickle.scala.syntax.task._

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

case class VaultConfig(wsClient: WSClient, token: Task[String])
case class AppId(app_id: String, user_id: String)

object VaultConfig {

  def apply(client: WSClient, appId: AppId)(implicit ec: ExecutionContext): VaultConfig =
    VaultConfig(client,
                client.path("auth/app-id/login").
                  post(appId.asJson).
                  toTask.
                  acceptStatusCodes(200).
                  extractFromJson[String](_.downField("auth").downField("client_token")))

  def apply(wsClient: WSClient, token: String): VaultConfig =
    VaultConfig(wsClient, Task.now(token))
}



case class WSClient(server: URL,
                    version: String = "v1") {
   def path(p: String): Req =
     url(s"${server.toString}/$version/$p").
       setContentType("application/json", "UTF-8")
}


