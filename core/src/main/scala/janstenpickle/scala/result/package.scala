package janstenpickle.scala

import cats.data.EitherT

import scala.concurrent.{Await, ExecutionContext, Future}
import cats.implicits._

import scala.concurrent.duration._
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

  implicit class AsyncResultOps[F, R](f: AsyncResult[F, R]) {
    def eiT: AsyncEitherT[F, R] = EitherT[Future, F, R](f)

    def attemptRun(implicit ec: ExecutionContext): Result[F, R] =
      Await.result(f, 1 minute)
  }

  implicit class AsyncResultOpsEitherT[F, R](f: Either[F, R]) {
    def eiT(implicit ec: ExecutionContext): AsyncEitherT[F, R] =
      EitherT.fromEither[Future](f)
  }

  implicit class AsyncResultOpsAny[F, R](r: R) {
    def eiT: AsyncEitherT[F, R] =
      EitherT.apply(Future.successful(Either right r))
  }
}