package janstenpickle.vault.auth.token

import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.vault.core.VaultSpec
import org.scalacheck.Test.Parameters
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import play.api.libs.json.Json

class TokenIT extends VaultSpec with ScalaCheck {
  import TokenIT._
  import VaultSpec._

  implicit val userFormat = Json.format[User]

  def is = step(setupUserAuth) ^
    s2"""
      Can get info on the admin token $testAdminToken
      Can authenticate users who have client IDs $clientIdUser
      Can authenticate users who do not have a client ID $plainUser
      """

  def setupUserAuth =
    rootConfig.authenticatedRequest("sys/auth/userpass")(_.post(Json.toJson(Map("type" -> "userpass"))).toTask).
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt

  lazy val underTest = Token(config)

  def testAdminToken = underTest.validate(adminToken).unsafePerformSyncAttempt must be_\/-.
    like { case a => a.clientId must beNone }

  def plainUser = testUserTokens(userGen(longerStrGen), (tokenResponse, username) =>
    tokenResponse.clientId must beNone and
    tokenResponse.username === Some(username.toLowerCase))

  def clientIdUser = testUserTokens(userGen(usernameGen), (tokenResponse, username) =>
    tokenResponse.clientId === TokenResponse.splitUsername(username).clientId.map(_.toLowerCase) and
    tokenResponse.username === TokenResponse.splitUsername(username).username.map(_.toLowerCase))

  def testUserTokens(gen: Gen[User], test: (TokenResponse, String) => MatchResult[Any]) =
    Prop.forAllNoShrink(gen) { user =>
      (rootConfig.authenticatedRequest(s"auth/userpass/users/${user.username}")(
        _.post(Json.toJson(user))
      ).acceptStatusCodes(204).unsafePerformSyncAttempt must be_\/-) and
      (config.wsClient.path(s"auth/userpass/login/${user.username}").
        post(Json.toJson(Map("username" -> user.username, "password" -> user.password))).
        toTask.
        acceptStatusCodes(200).
        extractFromJson[String](_ \ "auth" \ "client_token").
        unsafePerformSyncAttempt must be_\/-.
        like { case token => underTest.validate(token).unsafePerformSyncAttempt must be_\/-.
          like { case a => test(a, user.username) } })
    }
}

object TokenIT {
  case class User(username: String, password: String, ttl: String)

  val longerStrGen = Gen.alphaStr.suchThat(_.length >= 3)
  val usernameGen = for {
    clientId <- longerStrGen
    username <- longerStrGen
  } yield s"$clientId${TokenResponse.Separator}$username"

  def userGen(usernameGen: Gen[String]): Gen[User] = for {
    username <- usernameGen
    password <- longerStrGen
    ttl <- Gen.posNum[Int].suchThat(_ > 10)
  } yield User(username, password, s"${ttl}s")
}
