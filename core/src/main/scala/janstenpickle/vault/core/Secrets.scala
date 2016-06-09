package janstenpickle.vault.core

import com.ning.http.client.Response
import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.request._
import janstenpickle.scala.syntax.response._

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

case class Secrets(config: VaultConfig, backend: String) {
  def get(key: String, subKey: String = "value")(implicit ec: ExecutionContext): Task[String] =
    getAll(key).flatMap(_.get(subKey).toTask(s"Cannot find sub-key $subKey in secret $key"))

  def getAll(key: String)(implicit ec: ExecutionContext): Task[Map[String, String]] =
    config.authenticatedRequest(path(key))(_.get).
      execute.
      acceptStatusCodes(200).
      extractFromJson[Map[String, String]](_.downField("data"))

  def set(key: String, value: String)(implicit ec: ExecutionContext): Task[Response] =
    set(key, "value", value)

  def set(key: String, subKey: String, value: String)(implicit ec: ExecutionContext): Task[Response] =
    set(key, Map(subKey -> value))

  def set(key: String, values: Map[String, String])(implicit ec: ExecutionContext): Task[Response] =
    config.authenticatedRequest(path(key))(_.post(values)).
      execute.
      acceptStatusCodes(204)

  def list(implicit ec: ExecutionContext): Task[List[String]] =
    config.authenticatedRequest(backend)(_.addQueryParameter("list", true.toString).get).
      execute.
      acceptStatusCodes(200).
      extractFromJson[List[String]](_.downField("data").downField("keys"))

  def path(key: String): String = s"$backend/$key"
}
