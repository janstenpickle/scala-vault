package janstenpickle.vault.auth

import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.vault.core.VaultSpec
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import play.api.libs.json.Json

import scalaz.\/

class TokenIT extends VaultSpec with ScalaCheck {
  import TokenIT._

  implicit val userFormat = Json.format[User]

  def is = step(setupUserAuth) ^
    s2"""
      Can get info on the admin token $testAdminToken
      Can authenticate users who have client IDs $testAuth
      Fails to authenticate tokens which have expired $testExpiry
      """

  def setupUserAuth =
    rootConfig.authenticatedRequest(s"sys/auth/$clientId")(_.post(Json.toJson(Map("type" -> "userpass"))).toTask).
      acceptStatusCodes(200, 204).
      unsafePerformSyncAttempt

  lazy val underTest = Token(config)

  def testAdminToken = underTest.validate(adminToken).unsafePerformSyncAttempt must be_\/-

  def testAuth = testUserTokens(userGen(), (resp, user) => resp must be_\/-.
    like { case a =>
      a.username === Some(user.username.toLowerCase) and
      a.client === Some(clientId) and
      (a.ttl must beLessThanOrEqualTo(user.getTtl))})

  def testExpiry = testUserTokens(userGen(Gen.chooseNum[Int](1, 1)), (resp, user) => resp must be_-\/, Some(1500))

  def testUserTokens(userGen: Gen[User],
                     test: (Throwable \/ TokenResponse, User) => MatchResult[Any],
                     sleep: Option[Int] = None) =
    Prop.forAllNoShrink(userGen) { user =>
      val userCreation = rootConfig.authenticatedRequest(s"auth/$clientId/users/${user.username}")(
        _.post(Json.toJson(user))
      ).acceptStatusCodes(204).unsafePerformSyncAttempt must be_\/-

      val userAuth = config.wsClient.path(s"auth/$clientId/login/${user.username}").
        post(Json.toJson(Map("username" -> user.username, "password" -> user.password))).
        toTask.
        acceptStatusCodes(200).
        extractFromJson[String](_ \ "auth" \ "client_token").
        unsafePerformSyncAttempt

      sleep.foreach(Thread.sleep(_))

      userCreation and
      (userAuth must be_\/-.like { case token => test(underTest.validate(token).unsafePerformSyncAttempt, user) })
    }

}

object TokenIT {
  import VaultSpec._

  val clientId = "nic-cage"

  case class User(username: String, password: String, ttl: String, max_ttl: String) {
    def getTtl: Int = ttl.dropRight(1).toInt
  }

  val usernameGen = for {
    clientId <- longerStrGen
    username <- longerStrGen
  } yield s"$clientId${TokenResponse.Separator}$username"

  def userGen(ttlGen: Gen[Int] = Gen.posNum[Int]): Gen[User] = for {
    username <- longerStrGen
    password <- longerStrGen
    ttl <- ttlGen
  } yield User(username, password, s"${ttl}s", s"${ttl}s")
}
