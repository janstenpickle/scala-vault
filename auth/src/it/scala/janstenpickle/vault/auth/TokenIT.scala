package janstenpickle.vault.auth

import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.response._
import janstenpickle.scala.syntax.request._
import janstenpickle.vault.core.VaultSpec
import janstenpickle.vault.manage.Auth
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult

import io.circe.generic.auto._
import io.circe.syntax._

import scalaz.\/

class TokenIT extends VaultSpec with ScalaCheck {
  import TokenIT._

  def is = step(setupUserAuth) ^
    s2"""
      Can get info on the admin token $testAdminToken
      Can authenticate users who have client IDs $testAuth
      Fails to authenticate tokens which have expired $testExpiry
      """

  lazy val authAdmin = Auth(config)
  lazy val underTest = Token(config)

  def setupUserAuth =
    authAdmin.enable("userpass", Some(clientId)).unsafePerformSyncAttempt

  def testAdminToken = underTest.lookup(adminToken).unsafePerformSyncAttempt must be_\/-

  def testAuth = testUserTokens(userGen(), (resp, user) => resp must be_\/-.
    like { case a =>
      a.username === Some(user.username.toLowerCase) and
      a.client === Some(clientId) and
      (a.ttl must beLessThanOrEqualTo(user.getTtl))})

  def testExpiry = testUserTokens(userGen(Gen.chooseNum[Int](1, 1)), (resp, user) => resp must be_-\/, Some(1500))

  def testUserTokens(userGen: Gen[User],
                     test: (Throwable \/ LookupResponse, User) => MatchResult[Any],
                     sleep: Option[Int] = None) =
    Prop.forAllNoShrink(userGen) { user =>
      val userCreation = rootConfig.authenticatedRequest(s"auth/$clientId/users/${user.username}")(
        _.post(user.asJson)
      ).execute.acceptStatusCodes(204).unsafePerformSyncAttempt must be_\/-

      val userAuth = config.wsClient.path(s"auth/$clientId/login/${user.username}").
        post(Map("username" -> user.username, "password" -> user.password)).
        toTask.
        acceptStatusCodes(200).
        extractFromJson[String](_.downField("auth").downField("client_token")).
        unsafePerformSyncAttempt

      sleep.foreach(Thread.sleep(_))

      userCreation and
      (userAuth must be_\/-.like { case token => test(underTest.lookup(token).unsafePerformSyncAttempt, user) })
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
  } yield s"$clientId${LookupResponse.Separator}$username"

  def userGen(ttlGen: Gen[Int] = Gen.posNum[Int]): Gen[User] = for {
    username <- longerStrGen
    password <- longerStrGen
    ttl <- ttlGen
  } yield User(username, password, s"${ttl}s", s"${ttl}s")
}
