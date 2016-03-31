package janstenpickle.scala.syntax

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scalaz.concurrent.Task
import scalaz.syntax.either._

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
