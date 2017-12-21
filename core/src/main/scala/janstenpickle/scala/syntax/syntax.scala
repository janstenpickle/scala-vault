package janstenpickle.scala.syntax

import com.ning.http.client.Response
import dispatch.{Http, Req}
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import janstenpickle.vault.core.VaultConfig
import janstenpickle.scala.result._
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal

object OptionSyntax {
  implicit class ToTuple[T](opt: Option[T]) {
    def toMap(key: String): Map[String, T] =
      opt.fold[Map[String, T]](Map.empty)(v => Map(key -> v))
  }
}

object AsyncResultSyntax {
  implicit class FutureToAsyncResult[T](future: Future[T])
  (implicit ec: ExecutionContext) {
    def toAsyncResult: AsyncResult[String, T] = {
      future.map(Result pure[String, T] _).recover {
        case NonFatal(e) => Result fail[String, T] e.getMessage
      }
    }
  }

  implicit class ReqToAsyncResult(req: Req)
  (implicit ec: ExecutionContext) {
    def toAsyncResult: AsyncResult[String, Response] = Http(req).toAsyncResult
  }

  implicit def toAsyncResult[T](future: scala.concurrent.Future[T])
  (implicit ec: ExecutionContext): AsyncResult[String, T] =
    future.toAsyncResult
}

object VaultConfigSyntax {

  final val VaultTokenHeader = "X-Vault-Token"

  implicit class RequestHelper(config: VaultConfig) {
    def authenticatedRequest(path: String)(req: Req => Req)
    (implicit ec: ExecutionContext): AsyncResult[String, Req] ={
      val r = for {
        token <- config.token.eiT
      } yield req(config.wsClient.path(path).setHeader(VaultTokenHeader, token))
      r.value
    }
  }
}

object JsonSyntax {

  implicit class JsonHandler(json: AsyncResult[String, Json]) {
    def extractFromJson[T](jsonPath: HCursor => ACursor = _.downArray)
    (
      implicit decode: Decoder[T],
      ec: ExecutionContext
    ): AsyncResult[String, T] = {
      val r = for {
        j <- json.eiT
        e <- decode.tryDecode(jsonPath(j.hcursor)).leftMap(_.message).eiT
      } yield e
      r.value
    }

  }
}

object ResponseSyntax {
  import JsonSyntax._

  implicit class ResponseHandler(resp: AsyncResult[String, Response]) {
    def acceptStatusCodes(codes: Int*)
    (implicit ec: ExecutionContext): AsyncResult[String, Response] = {
      val r = for {
        response <- resp.eiT
        r <- Result.cond(
          test = codes.contains(response.getStatusCode),
          right = response,
          left = s"Received failure response from server:" +
            s" ${response.getStatusCode}\n ${response.getResponseBody}"
        ).eiT
      } yield r
      r.value
    }

    def extractJson(implicit ec: ExecutionContext):
      AsyncResult[String, Json] = {
      val r = for {
        response <- resp.eiT
        r <- parse(response.getResponseBody).leftMap(_.message).eiT
      } yield r
      r.value
    }


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
    import AsyncResultSyntax._
    def execute: AsyncResult[String, Response] = {
      val r = for {
        request <- req.eiT
        response <- Http(request).toAsyncResult.eiT
      } yield response
      r.value
    }

  }

  implicit class HttpOps(req: Req) {
    def get: Req = req.GET
    def post(body: String): Req = req.setBody(body).POST
    def post(json: Json): Req = post(json.noSpaces)
    def post(map: Map[String, String]): Req = post(map.asJson)
    def delete: Req = req.DELETE
  }
}
