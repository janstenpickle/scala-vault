package janstenpickle.vault.core

import java.net.URL

import janstenpickle.scala.result._
import janstenpickle.scala.syntax.AsyncResultSyntax._
import janstenpickle.scala.syntax.SyntaxRequest._
import janstenpickle.scala.syntax.ResponseSyntax._
import janstenpickle.scala.syntax.VaultConfigSyntax._
import org.scalacheck.Gen
import org.specs2.Specification
import org.specs2.matcher.EitherMatchers
import org.specs2.specification.core.Fragments

import scala.concurrent.ExecutionContext
import scala.io.Source


trait VaultSpec extends Specification with EitherMatchers {
  implicit val errConverter: Throwable => String = _.getMessage
  implicit val ec: ExecutionContext = ExecutionContext.global

  lazy val rootToken: String = Source.fromFile("/tmp/.root-token").mkString.trim
  lazy val roleId:    String = Source.fromFile("/tmp/.role-id").mkString.trim
  lazy val secretId:  String = Source.fromFile("/tmp/.secret-id").mkString.trim

  lazy val rootConfig: VaultConfig = VaultConfig(
    WSClient(new URL("http://localhost:8200")), rootToken
  )
  lazy val badTokenConfig = VaultConfig(
    rootConfig.wsClient,
    "face-off"
  )
  lazy val config = VaultConfig(
    rootConfig.wsClient,
    AppRole(roleId, secretId)
  )
  lazy val badServerConfig = VaultConfig(
    WSClient(new URL("http://nic-cage.xyz")),
    "con-air"
  )

  def check = config.token.attemptRun should beRight

  override def map(fs: => Fragments) =
    s2"""
      Can receive a token for an AppRole $check
    """ ^
    fs
}

object VaultSpec {
  val longerStrGen = Gen.alphaStr.suchThat(_.length >= 3)
  val strGen = Gen.alphaStr.suchThat(_.nonEmpty)
}
