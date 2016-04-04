package janstenpickle.vault.core

import java.net.URL

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import org.scalacheck.Gen
import org.specs2.Specification
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.specification.core.Fragments
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scala.io.Source

trait VaultSpec extends Specification with DisjunctionMatchers {
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()

  val appId = "nic"
  val userId = "cage"

  lazy val adminToken = Source.fromFile("/tmp/.vault-token").mkString.trim

  lazy val rootConfig: VaultConfig = VaultConfig(WSClientWrapper(new URL("http://localhost:8200")), adminToken)
  lazy val config = VaultConfig(rootConfig.wsClient, AppId(appId, userId))
  lazy val badTokenConfig = VaultConfig(rootConfig.wsClient, "face-off")
  lazy val badServerConfig = VaultConfig(WSClientWrapper(new URL("http://nic-cage.xyz")), "con-air")

  def init = {
    rootConfig.authenticatedRequest("sys/auth/app-id")(_.post(Json.toJson(Map("type" -> "app-id"))).toTask).
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt
    rootConfig.
      authenticatedRequest(s"auth/app-id/map/app-id/$appId")(_.post(Json.toJson(Map("value" -> "root",
                                                                                    "display_name" -> appId))).toTask).
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt
    rootConfig.
      authenticatedRequest(s"auth/app-id/map/user-id/$userId")(_.post(Json.toJson(Map("value" -> appId))).toTask).
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt
  }

  override def map(fs: => Fragments) =
    step(init) ^
    s2"""
      Can receive a token for an app ID ${config.token.unsafePerformSyncAttempt must be_\/-}
    """ ^
    fs ^
    step(config.wsClient.underlying.close()) ^
    step(rootConfig.wsClient.underlying.close()) ^
    step(badTokenConfig.wsClient.underlying.close()) ^
    step(badServerConfig.wsClient.underlying.close())
}

object VaultSpec {
  val strGen = Gen.alphaStr.suchThat(!_.isEmpty)
}
