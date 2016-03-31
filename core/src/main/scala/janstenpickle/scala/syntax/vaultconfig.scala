package janstenpickle.scala.syntax

import janstenpickle.vault.VaultConfig
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

object vaultconfig {
  final val VaultTokenHeader = "X-Vault-Token"

  implicit class RequestHelper(config: VaultConfig) {
    def authenticatedRequest(path: String)(req: WSRequest => Task[WSResponse])
                            (implicit ec: ExecutionContext): Task[WSResponse] =
      config.token.flatMap[WSResponse](token =>
        req(config.wsClient.path(path).withHeaders(VaultTokenHeader -> token))
      )
  }
}
