package janstenpickle.scala.syntax

import janstenpickle.vault.core.VaultConfig
import play.api.libs.ws.{WSResponse, WSRequest}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalaz.\/
import scalaz.concurrent.Task
import scalaz.syntax.either._
import play.api.libs.json.{Json, JsReadable, JsValue, Reads}

object jsvalue {
  import janstenpickle.scala.syntax.task._

  implicit class JsonHandler(json: Task[JsValue]) {
    def extractFromJson[T](op: JsValue => JsReadable, errorMsg: Option[String] = None)
                          (implicit fjs: Reads[T], ec: ExecutionContext): Task[T] =
      json.flatMap[T](jsValue =>
        op(jsValue).asOpt[T].toTask(errorMsg.getOrElse(s"Could not extract value from JSON: $jsValue"))
      )
  }
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

  implicit def toTask[T](future: scala.concurrent.Future[T])(implicit ec: ExecutionContext): Task[T] = future.toTask
}


object vaultconfig {
  final val VaultTokenHeader = "X-Vault-Token"

  implicit class RequestHelper(config: VaultConfig) {
    def authenticatedRequest(path: String)(req: WSRequest => Task[WSResponse])
                            (implicit ec: ExecutionContext): Task[WSResponse] =
      config.token.flatMap[WSResponse](token =>
        req(config.wsClient.path(path).withHeaders(VaultTokenHeader -> token))
      )
  }
}

object wsresponse {
  import janstenpickle.scala.syntax.jsvalue._

  implicit class ResponseHandler(resp: Task[WSResponse]) {
    def acceptStatusCodes(codes: Int*)(implicit ec: ExecutionContext): Task[WSResponse] =
      resp.flatMap(wsResponse =>
        if (codes.contains(wsResponse.status)) Task.now(wsResponse)
        else Task.fail(
          new RuntimeException(s"Received failure response from server: ${wsResponse.status} \n ${wsResponse.body}")
        )
      )

    def extractJson(implicit ec: ExecutionContext): Task[JsValue] =
      resp.flatMap(wsResponse =>
        Task.fromDisjunction(\/.fromTryCatchNonFatal(Json.parse(wsResponse.body)))
      )

    def extractFromJson[T](op: JsValue => JsReadable = identity, errorMsg: Option[String] = None)
                          (implicit fjs: Reads[T], ec: ExecutionContext): Task[T] =
      resp.extractJson.extractFromJson[T](op, errorMsg)
  }
}
