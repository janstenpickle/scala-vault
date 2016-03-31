package janstenpickle.scala.syntax

import janstenpickle.scala.syntax.jsvalue._
import play.api.libs.json.{JsLookupResult, JsValue, Json, Reads}
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext
import scalaz.\/
import scalaz.concurrent.Task

object wsresponse {
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

    def extractFromJson[T](op: JsValue => JsLookupResult, errorMsg: Option[String] = None)
                          (implicit fjs: Reads[T], ec: ExecutionContext): Task[T] =
      resp.extractJson.extractFromJson[T](op, errorMsg)
  }
}
