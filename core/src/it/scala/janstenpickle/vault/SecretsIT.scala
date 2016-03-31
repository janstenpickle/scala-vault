package janstenpickle.vault

class SecretsIT extends VaultSpec {

  override def is =
    s2"""
      Can set a secret in vault $set
      Can set and get a secret in vault $get
      Can list keys $list
      Can set multiple subkeys $setMulti
      Can set and get multiple subKeys $getSetMulti
      Cannot get non-existent key $failGet
      Fails to perform actions on a non-vault server $failSetBadServer
      Fails to perform actions with a bad token $failSetBadToken
      """

  lazy val good = Secrets(config)
  lazy val badToken = Secrets(badTokenConfig)
  lazy val badServer = Secrets(badServerConfig)

  def set = good.set("nic", "cage").unsafePerformSyncAttempt must be_\/-

  def get =
    (good.set("kim", "coppola").unsafePerformSyncAttempt must be_\/-) and
    (good.get("kim").unsafePerformSyncAttempt must be_\/-.like { case a => a === "coppola" })

  def list =
    (good.set("nic", "cage").unsafePerformSyncAttempt must be_\/-) and
    (good.set("cage", "coppola").unsafePerformSyncAttempt must be_\/-) and
    (good.list.unsafePerformSyncAttempt must be_\/-.like { case a => a must containAllOf(Seq("nic", "cage")) })

  def setMulti =
    good.set("nicolas-cage", Map("nic" -> "cage", "best" -> "actor")).unsafePerformSyncAttempt must be_\/-

  def getSetMulti = {
    val testData = Map("kim" -> "cage", "nic" -> "coppola")
    (good.set("nic-kim-cage", testData).unsafePerformSyncAttempt must be_\/-) and
    (good.getAll("nic-kim-cage").unsafePerformSyncAttempt must be_\/-.like { case a => a === testData })
  }

  def failGet = good.get("john").unsafePerformSyncAttempt must be_-\/.
    like { case ex => ex.getMessage must contain("Received failure response from server: 404") }

  def failSetBadServer = badServer.set("nic", "cage").unsafePerformSyncAttempt must be_-\/

  def failSetBadToken = badToken.set("nic", "cage").unsafePerformSyncAttempt must be_-\/
}
