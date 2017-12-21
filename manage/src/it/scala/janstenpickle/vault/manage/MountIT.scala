package janstenpickle.vault.manage

import com.ning.http.client.Response
import com.ning.http.client.providers.jdk.JDKResponse
import janstenpickle.vault.core.VaultSpec
import janstenpickle.vault.manage.Model.{Mount, MountConfig}
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import janstenpickle.scala.syntax.AsyncResultSyntax._
import janstenpickle.scala.result._

class MountIT extends VaultSpec with ScalaCheck {
  import MountIT._
  import VaultSpec._

  def is =
    s2"""
      Can enable, remount and disable a valid mount $happy
      Can enable, list and then disable valid mounts $listSuccess
      Cannot enable an invalid mount type $enableFail
    """

  lazy val underTest = new Mounts(config)

  def happy = Prop.forAllNoShrink(
    mountGen,
    longerStrGen,
    longerStrGen,
    Gen.option(longerStrGen))((mount, mountPoint, remountPoint, desc) => {
      (underTest.mount(mount.`type`, Some(mountPoint), desc, Some(mount))
      .attemptRun must beRight) and
      (underTest.remount(mountPoint, remountPoint)
        .attemptRun must beRight) and
      (underTest.delete(remountPoint)
        .attemptRun must beRight) and
      (underTest.delete(mountPoint)
        .attemptRun must beRight)
    })

  def listSuccess = {
    processMountTypes((acc, mount) =>
      acc.right.flatMap(_ => underTest.mount(mount).attemptRun)
    ) must beRight
  } and {
    underTest.list.attemptRun must beRight.like {
      case a => a.map(_._2.`type`) must containAllOf(mountTypes)
    }
  } and {
    processMountTypes((acc, mount) =>
      acc.right.flatMap(_ => underTest.delete(mount).attemptRun)
    ) must beRight
  }

  def enableFail = Prop.forAllNoShrink(
    longerStrGen.suchThat(!mountTypes.contains(_)),
    longerStrGen,
    Gen.option(longerStrGen))((`type`, mount, desc) =>
      underTest.mount(`type`, Some(mount), desc)
        .attemptRun must beLeft
  )

}

object MountIT {
  import VaultSpec._

  val mountTypes = List(
    "aws", "cassandra", "consul", "generic",
    "mssql", "mysql", "pki", "postgresql", "ssh", "transit"
  )
  val mount = Gen.oneOf(mountTypes)
  val mounts = Gen.listOf(mountTypes).suchThat(_.nonEmpty)

  val mountGen = for {
    mountType <- mount
    description <- Gen.option(longerStrGen)
    defaultTtl <- Gen.option(Gen.posNum[Int])
    maxTtl <- Gen.option(Gen.posNum[Int])
    forceNoCache <- Gen.option(Gen.oneOf(true, false))
  } yield Mount(mountType, description, Some(MountConfig(defaultTtl, maxTtl, forceNoCache)))

  def processMountTypes(op: (Result[String, Response], String) => Result[String,
    Response]) =
      mountTypes.foldLeft[Result[String, Response]](Result.pure(new
        JDKResponse(null, null, null)))(op)

}
