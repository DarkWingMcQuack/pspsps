/** Independent library metadata; set these values before the first release. */
object ProjectInfo {
  private def configured(name: String, default: String): String =
    sys.env.get(name).map(_.trim).filter(_.nonEmpty).getOrElse(default)

  val owner = configured("PSPSPS_GITHUB_OWNER", "DarkWingMcQuack")
  val repository = configured("PSPSPS_REPOSITORY", "pspsps")
  val organization = configured("PSPSPS_ORGANIZATION", "io.github.darkwingmcquack")
  val maintainer = configured("PSPSPS_MAINTAINER", "pspsps contributors")
  val url = "https://github.com/" + owner + "/" + repository

  val isConfigured =
    owner != "your-github-handle" && organization != "io.github.your-github-handle"
}
