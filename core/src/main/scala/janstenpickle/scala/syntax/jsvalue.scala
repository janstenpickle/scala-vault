package janstenpickle.scala.syntax

import play.api.libs.json.{JsValue, JsLookupResult, Reads}

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task
import janstenpickle.scala.syntax.task._

object jsvalue {
  implicit class JsonHandler(json: Task[JsValue]) {
    def extractFromJson[T](op: JsValue => JsLookupResult, errorMsg: Option[String] = None)
                          (implicit fjs: Reads[T], ec: ExecutionContext): Task[T] =
      json.flatMap[T](jsValue =>
        op(jsValue).asOpt[T].toTask(errorMsg.getOrElse(s"Could not extract value from JSON: $jsValue"))
      )
  }
}
