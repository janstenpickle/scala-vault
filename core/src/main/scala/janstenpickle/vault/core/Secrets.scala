package janstenpickle.vault.core

import com.ning.http.client.Response
import janstenpickle.scala.syntax.SyntaxRequest._
import janstenpickle.scala.syntax.ResponseSyntax._
import janstenpickle.scala.syntax.VaultConfigSyntax._
import janstenpickle.scala.Result._
import cats.implicits._

import scala.concurrent.ExecutionContext

// scalastyle:off magic.number
case class Secrets(config: VaultConfig, backend: String) {
  def get(key: String, subKey: String = "value")
  (implicit ec: ExecutionContext): AsyncResult[String, String] = {
    val r = for {
      x <- getAll(key).eiT
      r <- Either.fromOption(
        x.get(subKey),
        s"Cannot find sub-key $subKey in secret $key"
      ).eiT
    } yield r
    r.value
  }


  def getAll(key: String)
  (implicit ec: ExecutionContext): AsyncResult[String, Map[String, String]] =
    config.authenticatedRequest(path(key))(_.get).
      execute.
      acceptStatusCodes(200).
      extractFromJson[Map[String, String]](_.downField("data"))

  def set(key: String, value: String)
  (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    set(key, "value", value)

  def set(key: String, subKey: String, value: String)
  (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    set(key, Map(subKey -> value))

  def set(key: String, values: Map[String, String])
  (implicit ec: ExecutionContext): AsyncResult[String, Response] =
    config.authenticatedRequest(path(key))(_.post(values)).
      execute.
      acceptStatusCodes(204)

  def list(implicit ec: ExecutionContext): AsyncResult[String, List[String]] =
    config.authenticatedRequest(backend)(
      _.addQueryParameter("list", true.toString).get).
      execute.
      acceptStatusCodes(200).
      extractFromJson[List[String]](_.downField("data").downField("keys"))

  def path(key: String): String = s"$backend/$key"
}
// scalastyle:on magic.number
