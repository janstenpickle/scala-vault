package janstenpickle.scala.syntax

import cats.data.Xor
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import com.ning.http.client.Response
import dispatch.{Http, Req}
import janstenpickle.vault.core.VaultConfig
import scalaz.\/

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalaz.concurrent.Task
import scalaz.syntax.either._
import scalaz.syntax.std.boolean._

object catsconversion {
  implicit def toDisjunction[A, B](xor: Xor[A, B]): A \/ B = xor.fold(_.left, _.right)
}

object option {
  implicit class ToTuple[T](opt: Option[T]) {
    def toMap(key: String): Map[String, T] =
      opt.fold[Map[String, T]](Map.empty)(v => Map(key -> v))
  }
}
 object task {
  implicit class FutureToTask[A](future: scala.concurrent.Future[A]) {
    def toTask(implicit ec: ExecutionContext): Task[A] =
      Task.async { register =>
        future.onComplete {
            case Success(v) => register(v.right)
            case Failure(ex) => register(ex.left)
          }
        }
  }

  implicit class OptionToTask[A](option: Option[A]) {
    def toTask(errorMsg: String)(implicit ec: ExecutionContext): Task[A] =
      option.fold[Task[A]](
        Task.fail(new RuntimeException(errorMsg))
      )(Task.now)
  }

  implicit class ReqToTask(req: Req) {
    def toTask(implicit ec: ExecutionContext): Task[Response] = Http(req).toTask
  }

  implicit def toTask[T](future: scala.concurrent.Future[T])(implicit ec: ExecutionContext): Task[T] = future.toTask
}

object vaultconfig {
  final val VaultTokenHeader = "X-Vault-Token"

  implicit class RequestHelper(config: VaultConfig) {
    def authenticatedRequest(path: String)(req: Req => Req)
                            (implicit ec: ExecutionContext): Task[Req] =
      config.token.map[Req](token =>
        req(config.wsClient.path(path).setHeader(VaultTokenHeader, token))
      )
  }
}

object json {
  import catsconversion._

  implicit class JsonHandler(json: Task[Json]) {
    def extractFromJson[T](jsonPath: HCursor => ACursor = _.acursor)(implicit decode: Decoder[T], ec: ExecutionContext): Task[T] =
      json.flatMap[T](j => Task.fromDisjunction(decode.tryDecode(jsonPath(j.hcursor)).leftMap(new RuntimeException(_))))
  }
}

object response {
  import json._
  import catsconversion._

  implicit class ResponseHandler(resp: Task[Response]) {
    def acceptStatusCodes(codes: Int*)(implicit ec: ExecutionContext): Task[Response] =
      resp.flatMap(response =>
        codes.contains(response.getStatusCode).option(Task.now(response)).getOrElse(
          Task.fail(
            new RuntimeException(s"Received failure response from server: ${response.getStatusCode} \n ${response.getResponseBody}")
          )
        )
      )

    def extractJson(implicit ec: ExecutionContext): Task[Json] =
      resp.flatMap(response =>
        Task.fromDisjunction(parse(response.getResponseBody).leftMap(new RuntimeException(_)))
      )

    def extractFromJson[T](jsonPath: HCursor => ACursor = _.acursor)
                          (implicit decode: Decoder[T], ec: ExecutionContext): Task[T] =
      resp.extractJson.extractFromJson[T](jsonPath)
  }
}

object request {
  import task._

  implicit class ExecuteRequest(req: Task[Req])(implicit ec: ExecutionContext) {
    def execute: Task[Response] =
      req.flatMap(Http(_).toTask)
  }

  implicit class HttpOps(req: Req) {
    def get: Req = req.GET
    def post(body: String): Req = req.setBody(body).POST
    def post(json: Json): Req = post(json.noSpaces)
    def post(map: Map[String, String]): Req = post(map.asJson)
    def delete: Req = req.DELETE
  }
}

