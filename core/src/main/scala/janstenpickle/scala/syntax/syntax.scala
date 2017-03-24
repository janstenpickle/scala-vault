package janstenpickle.scala.syntax

import com.ning.http.client.Response
import dispatch.{Http, Req}
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import janstenpickle.vault.core.VaultConfig
import uscala.concurrent.result.AsyncResult
import uscala.result.Result
import uscala.result.Result.{Fail, Ok}

import scala.concurrent.{ExecutionContext, Future}

object CatsConversion {
  implicit def toResult[A, B](xor: Either[A, B]): Result[A, B] = xor.fold(
    Result.fail, Result.ok
  )


  import cats.Applicative
  import cats.syntax.cartesian._

  type Shit[A] = Result[List[String], A]

  implicit def validatedApplicative: Applicative[Shit] =
    new Applicative[Shit] {
      def ap[A, B](f: Shit[A => B])(fa: Shit[A]): Shit[B] =
        (fa, f) match {
          case (Ok(a), Ok(fab)) => Ok(fab(a))
          case (Fail(e1), Fail(e2)) => Fail(e1 ++ e2)
          case (Ok(_), i@Fail(_)) => i
          case (i@Fail(_), Ok(_)) => i
        }

      def pure[A](x: A): Shit[A] = Result.ok(x)

      override def product[A, B](
        fa: Shit[A],
        fb: Shit[B]
      ): Shit[(A, B)] = ap(map(fa)(a => (b: B) => (a, b)))(fb)

      override def map[A, B](fa: Shit[A])(f: A => B): Shit[B] = ap(pure(f))(fa)
    }

  val shit = (
    Result.fail[List[String], Int](List("sdfsf")
  )
  |@| Result.fail[List[String], Int](List("3423rwfef"))
  |@| Result.ok[List[String], String]("sdfsf")).map((x, y, z) => (x, y, z))
}

object CatsOption {
  implicit class ToTuple[T](opt: Option[T]) {
    def toMap(key: String): Map[String, T] =
      opt.fold[Map[String, T]](Map.empty)(v => Map(key -> v))
  }
}

object CatsAsyncResult {
  implicit class FutureToAsyncResult[T](future: Future[T])
  (implicit ec: ExecutionContext) {
    def toAsyncResult: AsyncResult[String, T] = AsyncResult(
      future.map(Result.ok)
    )
  }

  implicit class ReqToAsyncResult(req: Req)
  (implicit ec: ExecutionContext) {
    def toAsyncResult: AsyncResult[String, Response] = Http(req).toAsyncResult
  }

  implicit def toAsyncResult[T](future: scala.concurrent.Future[T])
  (implicit ec: ExecutionContext): AsyncResult[String, T] =
    future.toAsyncResult
}

object CatsVaultConfig {

  final val VaultTokenHeader = "X-Vault-Token"

  implicit class RequestHelper(config: VaultConfig) {
    def authenticatedRequest(path: String)(req: Req => Req)
    (implicit ec: ExecutionContext): AsyncResult[String, Req] =
      config.token.map[Req](token =>
        req(config.wsClient.path(path).setHeader(VaultTokenHeader, token))
      )
  }
}

object CatsJson {
  import CatsConversion._

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

object CatsResponse {
  import CatsConversion._
  import CatsJson._

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

object CatsRequest {

  implicit class ExecuteRequest(req: AsyncResult[String, Req])
  (implicit ec: ExecutionContext) {
    def execute: AsyncResult[String, Response] =
      req.flatMapF(Http(_))
  }

  implicit class HttpOps(req: Req) {
    def get: Req = req.GET
    def post(body: String): Req = req.setBody(body).POST
    def post(json: Json): Req = post(json.noSpaces)
    def post(map: Map[String, String]): Req = post(map.asJson)
    def delete: Req = req.DELETE
  }
}

