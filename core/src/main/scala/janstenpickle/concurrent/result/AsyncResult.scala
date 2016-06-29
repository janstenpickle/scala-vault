package janstenpickle.concurrent.result

import uscala.result.Result

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

final class AsyncResult[+A, +B](val underlying: Future[Result[A, B]]) extends Serializable {

  def flatMap[AA >: A, D](f: B => AsyncResult[AA, D])(implicit ec: ExecutionContext): AsyncResult[AA, D] =
    AsyncResult(
      underlying.flatMap(
        _.fold(
          AsyncResult.fail[AA, D](_).underlying,
          f(_).underlying
        )
      )
    )

  def flatMapR[AA >: A, D](f: B => Result[AA, D])(implicit ec: ExecutionContext): AsyncResult[AA, D] =
    AsyncResult(
      underlying.map(
        _.flatMap(f)
      )
    )

  def flatMapF[C](f: B => Future[C])(implicit ec: ExecutionContext): AsyncResult[A, C] =
    flatMap(f.andThen(x => AsyncResult.fromFuture(x)))

  def map[C](f: B => C)(implicit ec: ExecutionContext): AsyncResult[A, C] =
    AsyncResult(
      underlying.map(
        _.map(f)
      )
    )

  def mapOk[C](f: B => C)(implicit ec: ExecutionContext): AsyncResult[A, C] = map(f)

  def leftMap[C](f: A => C)(implicit ec: ExecutionContext): AsyncResult[C, B] =
    AsyncResult(
      underlying.map(
        _.mapFail(f)
      )
    )

  def mapFail[C](f: A => C)(implicit ec: ExecutionContext): AsyncResult[C, B] = leftMap(f)

  def bimap[C, D](fa: A => C, fb: B => D)(implicit ec: ExecutionContext): AsyncResult[C, D] =
    AsyncResult(
      underlying.map(
        _.bimap(fa, fb)
      )
    )

  def swap(implicit ec: ExecutionContext): AsyncResult[B, A] = AsyncResult(
    underlying.map(
      _.swap
    )
  )

  def fold[C](fa: A => C, fb: B => C)(implicit ec: ExecutionContext): Future[C] = underlying.map(_.fold(fa, fb))

  def attemptRunFor(duration: Duration): Result[Throwable, Result[A, B]] =
    Result.attempt(Await.result(underlying, duration))

  def attemptRunFor[AA >: A](f: Throwable => AA, duration: Duration)(implicit ec: ExecutionContext): Result[AA, B] =
    Result.attempt(Await.result(underlying, duration)).mapFail(f).flatMap(identity)

  def attemptRun(implicit ec: ExecutionContext): Result[Throwable, Result[A, B]] =
    attemptRunFor(Duration.Inf)

  def attemptRun[AA >: A](f: Throwable => AA)(implicit ec: ExecutionContext): Result[AA, B] =
    attemptRunFor(f, Duration.Inf)
}

object AsyncResult {

  def apply[A, B](f: Future[Result[A, B]]) = new AsyncResult(f)

  def fromFuture[A, B](f: Future[B])(implicit ex: ExecutionContext): AsyncResult[A, B] = AsyncResult(f.map(Result.ok))

  def fromResult[A, B](r: Result[A, B]): AsyncResult[A, B] = AsyncResult(Future.successful(r))

  def ok[A, B](b: B): AsyncResult[A, B] = fromResult(Result.ok(b))

  def fail[A, B](a: A): AsyncResult[A, B] = fromResult(Result.fail(a))

  def attempt[B](f: => B): AsyncResult[Throwable, B] =
    AsyncResult(Future.successful(Result.attempt(f)))

}
