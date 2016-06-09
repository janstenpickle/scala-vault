package janstenpickle.vault.manage

import com.ning.http.client.Response
import com.ning.http.client.providers.jdk.JDKResponse
import janstenpickle.vault.core.VaultSpec
import janstenpickle.vault.manage.Model.{Mount, MountConfig}
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck

import scalaz.\/
import scalaz.syntax.either._

class MountIT extends VaultSpec with ScalaCheck {
  import MountIT._
  import VaultSpec._

  def is =
    s2"""
      Can enable, remount and disable a valid mount $happy
      Can enable, list and then disable valid mounts $listSuccess
      Cannot disable an unmounted mount $disableFail
      Cannot enable an invalid mount type $enableFail
    """

  lazy val underTest = new Mounts(config)

  def happy = Prop.forAllNoShrink(mountGen,
                                  longerStrGen,
                                  longerStrGen,
                                  Gen.option(longerStrGen))((mount, mountPoint, remountPoint, desc) =>
    (underTest.mount(mount.`type`, Some(mountPoint), desc, Some(mount)).unsafePerformSyncAttempt must be_\/-) and
    (underTest.remount(mountPoint, remountPoint).unsafePerformSyncAttempt must be_\/-) and
    (underTest.delete(remountPoint).unsafePerformSyncAttempt must be_\/-)
  )

  def listSuccess = (processMountTypes((acc, mount) =>
    acc.flatMap(_ => underTest.mount(mount).unsafePerformSyncAttempt)
  ) must be_\/-) and (underTest.list.unsafePerformSyncAttempt must be_\/-.like {
     case a => a.map(_._2.`type`) must containAllOf(mountTypes)
   }) and (processMountTypes((acc, mount) =>
     acc.flatMap(_ => underTest.delete(mount).unsafePerformSyncAttempt)
  ) must be_\/-)

  def disableFail = Prop.forAllNoShrink(mount, longerStrGen, Gen.option(longerStrGen))((`type`, mount, desc) =>
    underTest.delete(mount).unsafePerformSyncAttempt must be_-\/
  )

  def enableFail = Prop.forAllNoShrink(longerStrGen.suchThat(!mountTypes.contains(_)),
                                       longerStrGen,
                                       Gen.option(longerStrGen))((`type`, mount, desc) =>
    underTest.mount(`type`, Some(mount), desc).unsafePerformSyncAttempt must be_-\/
  )

}

object MountIT {
  import VaultSpec._

  val mountTypes = List("aws", "cassandra", "consul", "generic", "mssql", "mysql", "pki", "postgresql", "ssh", "transit")
  val mount = Gen.oneOf(mountTypes)
  val mounts = Gen.listOf(mountTypes).suchThat(_.nonEmpty)

  val mountGen = for {
    mountType <- mount
    description <- Gen.option(longerStrGen)
    ttl <- Gen.posNum[Int]
  } yield Mount(mountType, description, Some(MountConfig(ttl, ttl)))

  def processMountTypes(op: (Throwable \/ Response, String) => Throwable \/ Response) =
    mountTypes.foldLeft[Throwable \/ Response](new JDKResponse(null, null, null).right[Throwable])(op)
}


