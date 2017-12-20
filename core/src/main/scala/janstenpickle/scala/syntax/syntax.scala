package janstenpickle.scala.syntax

import dispatch.{Http, Req}
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import janstenpickle.vault.core.VaultConfig
import org.asynchttpclient.Response
import uscala.concurrent.result.AsyncResult
import uscala.result.Result

import scala.concurrent.{ExecutionContext, Future}

object ConversionSyntax {
  implicit def toResult[A, B](xor: Either[A, B]): Result[A, B] = xor.fold(
    Result.fail, Result.ok
  )
}

object OptionSyntax {
  implicit class ToTuple[T](opt: Option[T]) {
    def toMap(key: String): Map[String, T] =
      opt.fold[Map[String, T]](Map.empty)(v => Map(key -> v))
  }
}

object AsyncResultSyntax {
  implicit class FutureToAsyncResult[T](future: Future[T])
  (implicit ec: ExecutionContext) {
    def toAsyncResult: AsyncResult[String, T] = AsyncResult(
      future.map(Result.ok)
    )
  }

  implicit class ReqToAsyncResult(req: Req)
  (implicit ec: ExecutionContext) {
    def toAsyncResult: AsyncResult[String, Response] =
      Http.default(req)
        .toAsyncResult
  }

  implicit def toAsyncResult[T](future: scala.concurrent.Future[T])
  (implicit ec: ExecutionContext): AsyncResult[String, T] =
    future.toAsyncResult
}

object VaultConfigSyntax {

  final val VaultTokenHeader = "X-Vault-Token"

  implicit class RequestHelper(config: VaultConfig) {
    def authenticatedRequest(path: String)(req: Req => Req)
    (implicit ec: ExecutionContext): AsyncResult[String, Req] =
      config.token.map[Req](token =>
        req(config.wsClient.path(path).setHeader(VaultTokenHeader, token))
      )
  }
}

object JsonSyntax {
  import ConversionSyntax._

  implicit class JsonHandler(json: AsyncResult[String, Json]) {
    def extractFromJson[T](jsonPath: HCursor => ACursor = _.downArray)
    (
      implicit decode: Decoder[T],
      ec: ExecutionContext
    ): AsyncResult[String, T] =
      json.flatMapR(j => decode.tryDecode(
        jsonPath(j.hcursor)
      ).leftMap(_.message))
  }
}

object ResponseSyntax {
  import ConversionSyntax._
  import JsonSyntax._

  implicit class ResponseHandler(resp: AsyncResult[String, Response]) {
    def acceptStatusCodes(codes: Int*)
    (implicit ec: ExecutionContext): AsyncResult[String, Response] =
      resp.flatMapR(
        response =>
          if (codes.contains(response.getStatusCode)) {
            Result.ok(response)
          }
          else {
            Result.fail(
              s"Received failure response from server:" +
              s" ${response.getStatusCode}\n ${response.getResponseBody}"
            )
          }
      )

    def extractJson(implicit ec: ExecutionContext): AsyncResult[String, Json] =
      resp.flatMapR(response =>
        parse(response.getResponseBody).leftMap(_.message)
      )

    def extractFromJson[T](jsonPath: HCursor => ACursor = _.downArray)
    (
      implicit decode: Decoder[T],
      ec: ExecutionContext
    ): AsyncResult[String, T] =
      resp.extractJson.extractFromJson[T](jsonPath)
  }
}

object SyntaxRequest {

  implicit class ExecuteRequest(req: AsyncResult[String, Req])
  (implicit ec: ExecutionContext) {
    def execute: AsyncResult[String, Response] =
      req.flatMapF(Http.default.apply)
  }

  implicit class HttpOps(req: Req) {
    def get: Req = req.GET
    def post(body: String): Req = req.setBody(body).POST
    def post(json: Json): Req = post(json.noSpaces)
    def post(map: Map[String, String]): Req = post(map.asJson)
    def delete: Req = req.DELETE
  }
}
