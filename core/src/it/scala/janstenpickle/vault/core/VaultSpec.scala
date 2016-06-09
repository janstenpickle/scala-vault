package janstenpickle.vault.core

import java.net.URL

import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.response._
import janstenpickle.scala.syntax.request._
import org.scalacheck.Gen
import org.specs2.Specification
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.specification.core.Fragments

import scala.concurrent.ExecutionContext
import scala.io.Source


trait VaultSpec extends Specification with DisjunctionMatchers {
  implicit val ec: ExecutionContext = ExecutionContext.global

  val appId = "nic"
  val userId = "cage"

  lazy val adminToken = Source.fromFile("/tmp/.vault-token").mkString.trim

  lazy val rootConfig: VaultConfig = VaultConfig(WSClient(new URL("http://localhost:8200")), adminToken)
  lazy val config = VaultConfig(rootConfig.wsClient, AppId(appId, userId))
  lazy val badTokenConfig = VaultConfig(rootConfig.wsClient, "face-off")
  lazy val badServerConfig = VaultConfig(WSClient(new URL("http://nic-cage.xyz")), "con-air")

  def init = {
    rootConfig.authenticatedRequest("sys/auth/app-id")(_.post(Map("type" -> "app-id"))).
      execute.
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt
    rootConfig.
      authenticatedRequest(s"auth/app-id/map/app-id/$appId")(_.post(Map("value" -> "root",
                                                                        "display_name" -> appId))).
      execute.
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt
    rootConfig.
      authenticatedRequest(s"auth/app-id/map/user-id/$userId")(_.post(Map("value" -> appId))).
      execute.
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt
  }

  override def map(fs: => Fragments) =
    step(init) ^
    s2"""
      Can receive a token for an app ID ${config.token.unsafePerformSyncAttempt must be_\/-}
    """ ^
    fs
}

object VaultSpec {
  val longerStrGen = Gen.alphaStr.suchThat(_.length >= 3)
  val strGen = Gen.alphaStr.suchThat(_.nonEmpty)
}
