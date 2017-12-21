package janstenpickle.scala

import scala.concurrent.Future
import cats.implicits._


import scala.language.postfixOps

package object result {
  type Result[F, R] = Either[F, R]
  val Result: Either.type = Either

  implicit class ResultCompanionOps(dc: Either.type ){
    def pure[F, R](r: R): Either[F, R] = Either right r

    def fail[F, R](f: F): Either[F, R] = Either left f
  }

  type AsyncResult[F, R] = Future[Result[F, R]]

  type AsyncEitherT[F, R] = cats.data.EitherT[Future, F, R]

  object AsyncResult {
    def pure[F, R](r: R): AsyncResult[F, R] = {
      Future.successful(Either right r)
    }
  }


}
