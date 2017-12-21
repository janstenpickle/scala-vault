package janstenpickle.scala

import cats.data.EitherT

import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._

object Result {
  type Result[F, R] = Either[F, R]

  def pure[F, R](r: R): Either[F, R] = Either right r

  def fail[F, R](f: F): Either[F, R] = Either left f

  type AsyncResult[F, R] = Future[Result[F, R]]

  type AsyncEitherT[F, R] = cats.data.EitherT[Future, F, R]

  object AsyncResult {
    def pure[F, R](r: R): AsyncResult[F, R] = {
      Future.successful(Either right r)
    }
  }

  implicit class AsyncResultOps[F, R](f: AsyncResult[F, R]) {
    def eiT: AsyncEitherT[F, R] = EitherT[Future, F, R](f)
  }

  implicit class AsyncResultOpsEitherT[F, R](f: Either[F, R]) {
    def eiT(implicit ec: ExecutionContext): AsyncEitherT[F, R] = EitherT.fromEither[Future](f)
  }

  implicit class AsyncResultOpsAny[F, R](r: R) {
    def eiT: AsyncEitherT[F, R] = EitherT.apply(Future.successful(pure(r)))
  }
}
