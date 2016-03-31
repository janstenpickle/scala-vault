package janstenpickle.vault.auth.token

import janstenpickle.vault.core.VaultConfig
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.wsresponse._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
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

  private lazy val uname: Option[Username] = for {
    metadata <- meta
    username <- metadata.get("username")
  } yield splitUsername(username)

  lazy val clientId: Option[String] = uname.flatMap(_.clientId)
  lazy val username: Option[String] = uname.flatMap(_.username)
}

object TokenResponse {
  val Separator = '-'

  case class Username(username: Option[String], clientId: Option[String]) {
    def setUsername(un: String): Username = Username(Some(un), clientId)
    def setClientId(cId: String): Username = Username(username, Some(cId))
  }

  def limit(seq: Seq[String], limit: Int): Seq[String] =
    if (seq.size > 1)seq.take(limit) :+ seq.drop(limit).mkString(Separator.toString)
    else seq

  def splitUsername(username: String): Username =
    limit(username.split(Separator), 1).
      reverse.
      foldLeft(Username(None, None))( (acc, v) =>
        if (acc.username.isEmpty) acc.setUsername(v)
        else acc.setClientId(v)
      )
}