package janstenpickle.vault.auth

import janstenpickle.scala.syntax.AsyncResultSyntax._
import janstenpickle.vault.core.VaultSpec
import janstenpickle.vault.manage.Auth
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult

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
  lazy val authAdmin = Auth(config)
  lazy val userAdmin = janstenpickle.vault.manage.UserPass(config)

  def setupClient(client: String) = authAdmin.enable("userpass", Some(client))
    .attemptRun must beRight

  def setupUser(username: String, password: String, client: String) =
    userAdmin.create(username, password, 30, None, client)
      .attemptRun

  def removeClient(client: String) =
    authAdmin.disable(client).attemptRun must beRight

  def authPass = test((username, password, client, ttl) =>
                        setupClient(client) and
    (setupUser(username, password, client) must beRight) and
    (underTest.authenticate(username, password, ttl, client)
    .attemptRun must beRight) and
    removeClient(client)
  )

  // TODO: test below may fail rarely (e.g. client is same as badClientName)

  def badClient = test{ (username, password, client, ttl) =>
    val badClientName = "nic-kim-cage-client"
    setupClient(badClientName) and
    (setupUser(username, password, client) must beLeft) and
    (underTest.authenticate(username, password, ttl, client)
    .attemptRun must beLeft) and
    removeClient(badClientName)
  }

  def badUser = test{ (username, password, client, ttl) =>
    val badUserName = "nic-kim-cage-user"
    setupClient(client) and
    (setupUser(username, password, client) must beRight) and
    (underTest.authenticate(badUserName, password, ttl, client)
    .attemptRun must beLeft) and
    removeClient(client)
  }

  def badPassword = test{ (username, password, client, ttl) =>
    val badPasswordValue = "nic-kim-cage-password"
    setupClient(client) and
    (setupUser(username, password, client) must beRight) and
    (underTest.authenticate(username, badPasswordValue, ttl, client)
    .attemptRun must beLeft) and
    removeClient(client)
  }

  def test(op: (String, String, String, Int) => MatchResult[Any]) =
    Prop.forAllNoShrink(
      longerStrGen,
      longerStrGen,
      Gen.numStr.suchThat(_.nonEmpty), Gen.posNum[Int]
    )(op)
}
