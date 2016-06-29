package janstenpickle.vault.auth

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

  def setupClient(client: String) = authAdmin.enable("userpass", Some(client)).attemptRun(_.getMessage()) must beOk

  def setupUser(username: String, password: String, client: String) =
    userAdmin.create(username, password, 30, None, client).attemptRun(_.getMessage())

  def removeClient(client: String) =
    authAdmin.disable(client).attemptRun(_.getMessage()) must beOk

  def authPass = test((username, password, client, ttl) =>
                        setupClient(client) and
    (setupUser(username, password, client) must beOk) and
                        (underTest.authenticate(username, password, ttl, client).attemptRun(_.getMessage()) must beOk) and
                        removeClient(client)
  )

  def badClient = test{ (username, password, client, ttl) =>
    val badClient = "nic-kim-cage"
    setupClient(badClient) and
    (setupUser(username, password, client) must beFail) and
    (underTest.authenticate(username, password, ttl, client).attemptRun(_.getMessage()) must beFail) and
    removeClient(badClient)
  }

  def badUser = test{ (username, password, client, ttl) =>
    val badUser = "nic-kim-cage"
    setupClient(client) and
    (setupUser(username, password, client) must beOk) and
    (underTest.authenticate(badUser, password, ttl, client).attemptRun(_.getMessage()) must beFail) and
    removeClient(client)
  }

  def badPassword = test{ (username, password, client, ttl) =>
    val badPassword = "nic-kim-cage"
    setupClient(client) and
    (setupUser(username, password, client) must beOk) and
    (underTest.authenticate(username, badPassword, ttl, client).attemptRun(_.getMessage()) must beFail) and
    removeClient(client)
  }

  def test(op: (String, String, String, Int) => MatchResult[Any]) =
    Prop.forAllNoShrink(longerStrGen, longerStrGen, Gen.numStr.suchThat(_.nonEmpty), Gen.posNum[Int])(op)
}
