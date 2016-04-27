package janstenpickle.vault.auth

import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.vault.core.VaultSpec
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import play.api.libs.json.Json

class UserPassIT extends VaultSpec with ScalaCheck {
  import VaultSpec._

  override def is =
    s2"""
      Can authenticate a user against a specific "client" path $authPass
      Fails to authenticate a user $end
        against a bad "client" path $badClient
        with a non-existent username $badUser
        with a bad password $badPassword
    """

  lazy val underTest = UserPass(config.wsClient)

  def setupClient(client: String) =
    rootConfig.authenticatedRequest(s"sys/auth/$client")(_.post(Json.toJson(Map("type" -> "userpass")))).
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt must be_\/-

  def setupUser(username: String, password: String, client: String) =
    rootConfig.authenticatedRequest(s"auth/$client/users/$username")(
       _.post(Json.toJson(Map("password" -> password)))
     ).acceptStatusCodes(204).unsafePerformSyncAttempt

  def removeClient(client: String) =
    rootConfig.authenticatedRequest(s"sys/auth/$client")(_.delete()).
      acceptStatusCodes(204).
      unsafePerformSyncAttempt must be_\/-

  def authPass = test((username, password, client, ttl) =>
    setupClient(client) and
    (setupUser(username, password, client) must be_\/-) and
    (underTest.authenticate(username, password, ttl, client).unsafePerformSyncAttempt must be_\/-) and
    removeClient(client)
  )

  def badClient = test{ (username, password, client, ttl) =>
    val badClient = "nic-kim-cage"
    setupClient(badClient) and
    (setupUser(username, password, client) must be_-\/) and
    (underTest.authenticate(username, password, ttl, client).unsafePerformSyncAttempt must be_-\/) and
    removeClient(badClient)
  }

  def badUser = test{ (username, password, client, ttl) =>
    val badUser = "nic-kim-cage"
    setupClient(client) and
    (setupUser(username, password, client) must be_\/-) and
    (underTest.authenticate(badUser, password, ttl, client).unsafePerformSyncAttempt must be_-\/) and
    removeClient(client)
  }

  def badPassword = test{ (username, password, client, ttl) =>
    val badPassword = "nic-kim-cage"
    setupClient(client) and
    (setupUser(username, password, client) must be_\/-) and
    (underTest.authenticate(username, badPassword, ttl, client).unsafePerformSyncAttempt must be_-\/) and
    removeClient(client)
  }

  def test(op: (String, String, String, Int) => MatchResult[Any]) =
    Prop.forAllNoShrink(longerStrGen, longerStrGen, Gen.numStr.suchThat(!_.isEmpty), Gen.posNum[Int])(op)
}