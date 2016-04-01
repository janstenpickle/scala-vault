package janstenpickle.vault.auth

import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.vault.core.VaultSpec
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import play.api.libs.json.Json

class TokenIT extends VaultSpec with ScalaCheck {
  import TokenIT._

  implicit val userFormat = Json.format[User]

  def is = step(setupUserAuth) ^
    s2"""
      Can get info on the admin token $testAdminToken
      Can authenticate users who have client IDs $testUserTokens
      """

  def setupUserAuth =
    rootConfig.authenticatedRequest(s"sys/auth/$clientId")(_.post(Json.toJson(Map("type" -> "userpass"))).toTask).
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt

  lazy val underTest = Token(config)

  def testAdminToken = underTest.validate(adminToken).unsafePerformSyncAttempt must be_\/-

  def testUserTokens =
    Prop.forAllNoShrink(userGen) { user =>
      (rootConfig.authenticatedRequest(s"auth/$clientId/users/${user.username}")(
        _.post(Json.toJson(user))
      ).acceptStatusCodes(204).unsafePerformSyncAttempt must be_\/-) and
      (config.wsClient.path(s"auth/$clientId/login/${user.username}").
        post(Json.toJson(Map("username" -> user.username, "password" -> user.password))).
        toTask.
        acceptStatusCodes(200).
        extractFromJson[String](_ \ "auth" \ "client_token").
        unsafePerformSyncAttempt must be_\/-.
        like { case token => underTest.validate(token).unsafePerformSyncAttempt must be_\/-.
          like { case a => a.username === Some(user.username.toLowerCase) and a.clientId === Some(clientId) } })
    }
}

object TokenIT {
  val clientId = "nic-cage"

  case class User(username: String, password: String, ttl: String)

  val longerStrGen = Gen.alphaStr.suchThat(_.length >= 3)
  val usernameGen = for {
    clientId <- longerStrGen
    username <- longerStrGen
  } yield s"$clientId${TokenResponse.Separator}$username"

  val userGen: Gen[User] = for {
    username <- longerStrGen
    password <- longerStrGen
    ttl <- Gen.posNum[Int]
  } yield User(username, password, s"${ttl}s")
}
