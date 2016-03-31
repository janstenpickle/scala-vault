package janstenpickle.vault.core

import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

case class Secrets(config: VaultConfig)(implicit ec: ExecutionContext) {
  def get(key: String, subKey: String = "value"): Task[String] =
    getAll(key).flatMap(_.get(subKey).toTask(s"Cannot find sub-key $subKey in secret $key"))

  def getAll(key: String): Task[Map[String, String]] =
    config.authenticatedRequest(path(key))(_.get()).
      acceptStatusCodes(200).
      extractFromJson[Map[String, String]](_ \ "data")

  def set(key: String, value: String): Task[WSResponse] =
    set(key, "value", value)

  def set(key: String, subKey: String, value: String): Task[WSResponse] =
    set(key, Map(subKey -> value))

  def set(key: String, values: Map[String, String]): Task[WSResponse] =
    config.authenticatedRequest(path(key))(_.post(Json.toJson(values))).
      acceptStatusCodes(204)

  def list: Task[List[String]] =
    config.authenticatedRequest("secret")(_.withQueryString("list" -> true.toString).get()).
      acceptStatusCodes(200).
      extractFromJson[List[String]](_ \ "data" \ "keys")

  def path(key: String): String = s"secret/$key"
}
