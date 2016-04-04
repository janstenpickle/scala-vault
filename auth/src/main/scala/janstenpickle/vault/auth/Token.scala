package janstenpickle.vault.auth

import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.scala.syntax.task._
import janstenpickle.vault.core.VaultConfig
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scala.util.Try
import scalaz.concurrent.Task

case class Token(config: VaultConfig) {
  implicit val tokenResponseFormat = Json.format[TokenResponse]

  def validate(token: String)(implicit ec: ExecutionContext): Task[TokenResponse] =
    config.authenticatedRequest(s"auth/token/lookup/$token")(_.get()).
      acceptStatusCodes(200).
      extractFromJson[TokenResponse](_ \ "data")
}

case class TokenResponse(id: String,
                         policies: Option[List[String]],
                         path: String,
                         meta: Option[Map[String, String]],
                         display_name: Option[String],
                         num_uses: Int,
                         orphan: Boolean,
                         role: String,
                         ttl: Int) {
  import TokenResponse._

  lazy val client: Option[String] =
    meta.flatMap(_.get("client")).fold(Try(path.split(Separator)(1)).toOption)(Some(_))
  lazy val username: Option[String] = meta.flatMap(_.get("username"))
}

object TokenResponse {
  val Separator = '/'
}