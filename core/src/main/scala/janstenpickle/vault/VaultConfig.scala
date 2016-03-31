package janstenpickle.vault

import java.io.File
import java.net.URL
import java.security.KeyStore

import akka.stream.Materializer
import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.wsresponse._
import play.api.libs.json._
import play.api.libs.ws.ssl.{KeyStoreConfig, KeyManagerConfig, SSLConfig}
import play.api.libs.ws.{WSClientConfig, WSRequest}
import play.api.libs.ws.ahc.{AhcWSClientConfig, AhcWSClient}

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

case class VaultConfig(wsClient: WSClientWrapper, token: Task[String])

object VaultConfig {
  implicit val appIdFormat = Json.format[AppId]

  def apply(wsClient: WSClientWrapper, appId: AppId)(implicit ec: ExecutionContext): VaultConfig =
    VaultConfig(
      wsClient,
      wsClient.
      path("auth/app-id/login").
      post(Json.toJson(appId)).
      toTask.
      acceptStatusCodes(200).
      extractFromJson[String](_ \ "auth" \ "client_token",
                              Some("Could not find client token in response from vault"))
    )

  def apply(wsClient: WSClientWrapper, token: String): VaultConfig =
    VaultConfig(wsClient, Task.now(token))
}

case class AppId(app_id: String, user_id: String)

case class WSClientWrapper(server: URL,
                           version: String = "v1",
                           keyStoreType: Option[String] = None,
                           keyStoreFile: Option[File] = None,
                           keyStorePassword: Option[String] = None)(implicit materializer: Materializer) {

  val wsClient: AhcWSClient = AhcWSClient()

//    AhcWSClient(
//    AhcWSClientConfig(
//      wsClientConfig = WSClientConfig(
//        ssl = SSLConfig(
//          keyManagerConfig = KeyManagerConfig(
//            keyStoreConfigs = Seq(
//              KeyStoreConfig(storeType = keyStoreType.getOrElse(KeyStore.getDefaultType),
//                             filePath = keyStoreFile.map(_.getAbsolutePath),
//                             password = keyStorePassword)
//            )
//          )
//        )
//      )
//    )
//  )

  def path(p: String): WSRequest = wsClient.url(s"${server.toString}/$version/$p")
}


