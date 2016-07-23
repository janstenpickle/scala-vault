package janstenpickle.vault.auth

import io.circe.generic.auto._
import io.circe.syntax._
import janstenpickle.scala.syntax.request._
import janstenpickle.scala.syntax.response._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.vault.core.VaultConfig
import uscala.concurrent.result.AsyncResult

import scala.concurrent.ExecutionContext
import scala.util.Try

case class Token(config: VaultConfig) {
  import Token._

  def lookupPath(path: String)(implicit ec: ExecutionContext): AsyncResult[String, LookupResponse] =
    config.authenticatedRequest(path)(_.get).
      execute.
      acceptStatusCodes(200).
      extractFromJson[LookupResponse](_.downField("data"))

  def lookup(token: String)(implicit ec: ExecutionContext): AsyncResult[String, LookupResponse] =
    lookupPath(s"$Path/lookup/$token")

  def lookupSelf(implicit ec: ExecutionContext): AsyncResult[String, LookupResponse] =
    lookupPath(s"$Path/lookup-self")

  def create(policies: Option[List[String]] = None,
             meta: Option[Map[String, String]] = None,
             noParent: Option[Boolean] = None,
             noDefaultPolicy: Option[Boolean] = None,
             ttl: Option[Int] = None,
             numUses: Option[Int] = None)(implicit ec: ExecutionContext): AsyncResult[String, CreateResponse] =
    config.authenticatedRequest(s"$Path/create")(
      _.post(CreateRequest(policies, meta, noParent, noDefaultPolicy, ttl.map(x => s"${x}s"), numUses).asJson)
    ).execute.
      acceptStatusCodes(200).
      extractFromJson[CreateResponse](_.downField("auth"))
}

object Token {
  val Path = "auth/token"
}

case class CreateRequest(policies: Option[List[String]],
                         meta: Option[Map[String, String]],
                         no_parent: Option[Boolean],
                         no_default_policy: Option[Boolean],
                         ttl: Option[String],
                         num_uses: Option[Int])

case class CreateResponse(client_token: String,
                          policies: Option[List[String]],
                          metadata: Option[Map[String, String]],
                          lease_duration: Int,
                          renewable: Boolean)

case class LookupResponse(id: String,
                          policies: Option[List[String]],
                          path: String,
                          meta: Option[Map[String, String]],
                          display_name: Option[String],
                          num_uses: Int,
                          orphan: Boolean,
                          role: String,
                          ttl: Int) {
  import LookupResponse._

  lazy val client: Option[String] =
    meta.flatMap(_.get("client")).fold(Try(path.split(Separator)(1)).toOption)(Some(_))
  lazy val username: Option[String] = meta.flatMap(_.get("username"))
}

object LookupResponse {
  val Separator = '/'
}