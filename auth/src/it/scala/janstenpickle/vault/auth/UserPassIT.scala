package janstenpickle.vault.auth

import janstenpickle.scala.syntax.task._
import janstenpickle.scala.syntax.vaultconfig._
import janstenpickle.scala.syntax.wsresponse._
import janstenpickle.vault.core.VaultSpec
import org.specs2.ScalaCheck
import play.api.libs.json.Json

class UserPassIT extends VaultSpec with ScalaCheck {

  override def is =
    s2"""

      """


//  def setupClient(client: String) =
//    rootConfig.authenticatedRequest(s"sys/auth/$client")(_.post(Json.toJson(Map("type" -> "userpass"))).toTask).
//    acceptStatusCodes(200, 204).
//    unsafePerformSyncAttempt must be_\/-
//
//  def setupUser(username: String, password: String, client: String) =
//    rootConfig.authenticatedRequest(s"auth/$client/users/${username}")(
//      _.post(Json.toJson(user))
//    ).acceptStatusCodes(204).unsafePerformSyncAttempt must be_\/-
}
