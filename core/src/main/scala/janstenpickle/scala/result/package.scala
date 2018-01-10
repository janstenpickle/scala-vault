package janstenpickle.scala

import scala.concurrent.Future
import cats.implicits._


import scala.language.postfixOps

package object result {
  type Result[R] = Either[String, R]
  val Result: Either.type = Either

  implicit class ResultCompanionOps(dc: Either.type ){
    def pure[R](r: R): Either[String, R] = Either right r

    def fail[R](f: String): Either[String, R] = Either left f
  }

  type AsyncResult[R] = Future[Result[R]]

  type AsyncEitherT[R] = cats.data.EitherT[Future, String, R]

  object AsyncResult {
    def pure[R](r: R): AsyncResult[R] = {
      Future.successful(Either right r)
    }
  }


}
