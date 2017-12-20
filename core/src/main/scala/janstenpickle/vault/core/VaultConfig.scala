package janstenpickle.vault.core

import java.net.{URL, HttpURLConnection => status}
import java.nio.charset.Charset

import dispatch.{Req, url}
import io.circe.generic.auto._
import io.circe.syntax._
import janstenpickle.scala.syntax.AsyncResultSyntax._
import janstenpickle.scala.syntax.ResponseSyntax._
import janstenpickle.scala.syntax.SyntaxRequest._
import uscala.concurrent.result.AsyncResult

import scala.concurrent.ExecutionContext

case class VaultConfig(wsClient: WSClient, token: AsyncResult[String, String])

@deprecated("Vault 0.6.5 deprecated AppId in favor of AppRole", "0.4.0")
case class AppId(app_id: String, user_id: String)

case class AppRole(role_id: String, secret_id: String)

object VaultConfig {

  @deprecated("Vault 0.6.5 deprecated AppId in favor of AppRole", "0.4.0")
  def apply(client: WSClient, appId: AppId)
           (implicit ec: ExecutionContext): VaultConfig =
    VaultConfig(client,
      client.path("auth/app-id/login")
        .post(appId.asJson)
        .toAsyncResult
        .acceptStatusCodes(status.HTTP_OK)
        .extractFromJson[String](
        _.downField("auth").downField("client_token")
      )
    )

  def apply(client: WSClient, appRole: AppRole)
           (implicit ec: ExecutionContext): VaultConfig =
    VaultConfig(client,
      client.path("auth/approle/login")
        .post(appRole.asJson)
        .toAsyncResult
        .acceptStatusCodes(status.HTTP_OK)
        .extractFromJson[String](
        _.downField("auth").downField("client_token")
      )
    )

  def apply(wsClient: WSClient, token: String): VaultConfig =
    VaultConfig(wsClient, AsyncResult.ok[String, String](token))
}


case class WSClient(server: URL,
                    version: String = "v1") {
  def path(p: String): Req =
    url(s"${server.toString}/$version/$p")
      .setContentType("application/json", Charset.forName("UTF-8"))
}


