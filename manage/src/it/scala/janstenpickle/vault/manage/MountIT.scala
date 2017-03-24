package janstenpickle.vault.manage

import com.ning.http.client.Response
import com.ning.http.client.providers.jdk.JDKResponse
import janstenpickle.vault.core.VaultSpec
import janstenpickle.vault.manage.Model.{Mount, MountConfig}
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import uscala.result.Result

class MountIT extends VaultSpec with ScalaCheck {
  import MountIT._
  import VaultSpec._

  def is =
    s2"""
      Can enable, list, remount and disable a valid mount $happy
      Cannot enable an invalid mount type $enableFail
    """

  lazy val underTest = new Mounts(config)

  def happy = Prop.forAllNoShrink(
    mountGen,
    longerStrGen,
    longerStrGen,
    Gen.option(longerStrGen))((mount, mountPoint, remountPoint, desc) =>
      (underTest.mount(mount.`type`, Some(mountPoint), desc, Some(mount))
      .attemptRun(_.getMessage()) must beOk) and
      (underTest.remount(mountPoint, remountPoint)
      .attemptRun(_.getMessage()) must beOk) and
      (underTest.list
      .attemptRun(_.getMessage()) must beOk) and
      (underTest.delete(remountPoint)
      .attemptRun(_.getMessage()) must beOk) and
      (underTest.delete(mountPoint)
      .attemptRun(_.getMessage()) must beOk)
  )

  def enableFail = Prop.forAllNoShrink(
    longerStrGen.suchThat(!mountTypes.contains(_)),
    longerStrGen,
    Gen.option(longerStrGen))((`type`, mount, desc) =>
      underTest.mount(`type`, Some(mount), desc)
        .attemptRun(_.getMessage()) must beFail
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
    ttl <- Gen.posNum[Int]
    noCache <- Gen.oneOf(true, false)
  } yield Mount(mountType, description, Some(MountConfig(ttl, ttl, noCache)))

}
