package janstenpickle.vault.manage

import janstenpickle.vault.core.VaultSpec

class PolicyIT extends VaultSpec {

  override def is =
    s2"""
        ${step(shit())}
      """

  lazy val underTest = Policy(config)

  def shit() = {
    println(underTest.inspect("default").unsafePerformSyncAttempt.map((_ \ "rules" \ "path")))
  }
}
