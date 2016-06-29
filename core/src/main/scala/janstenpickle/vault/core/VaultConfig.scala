package janstenpickle.vault.core

import java.net.URL

import dispatch.{Req, url}
import io.circe.generic.auto._
import io.circe.syntax._
import janstenpickle.concurrent.result.AsyncResult
import janstenpickle.scala.syntax.request._
import janstenpickle.scala.syntax.response._
import janstenpickle.scala.syntax.asyncresult._

import scala.concurrent.ExecutionContext

case class VaultConfig(wsClient: WSClient, token: AsyncResult[String, String])
case class AppId(app_id: String, user_id: String)

object VaultConfig {

  def apply(client: WSClient, appId: AppId)(implicit ec: ExecutionContext): VaultConfig =
    VaultConfig(client,
                client.path("auth/app-id/login").
                  post(appId.asJson).
                  toAsyncResult.
                  acceptStatusCodes(200).
                  extractFromJson[String](_.downField("auth").downField("client_token")))

  def apply(wsClient: WSClient, token: String)(implicit ec: ExecutionContext): VaultConfig =
    VaultConfig(wsClient, AsyncResult.ok[String, String](token))
}



case class WSClient(server: URL,
                    version: String = "v1") {
   def path(p: String): Req =
     url(s"${server.toString}/$version/$p").
       setContentType("application/json", "UTF-8")
}


