import laika.ast.{Image, Path}
import laika.helium.config.{HeliumIcon, IconLink, ImageLink}
import org.typelevel.sbt.gha.{Permissions, PermissionValue}

ThisBuild / tlBaseVersion := "0.1"
ThisBuild / organization := ProjectInfo.organization
ThisBuild / organizationName := "pspsps contributors"
ThisBuild / startYear := Some(2026)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / homepage := Some(url(ProjectInfo.url))
ThisBuild / scmInfo := Some(
  ScmInfo(url(ProjectInfo.url), "scm:git:" + ProjectInfo.url + ".git")
)
ThisBuild / developers := List(tlGitHubDev(ProjectInfo.owner, ProjectInfo.maintainer))
ThisBuild / versionScheme := Some("early-semver")

// Deliberately retain the Scala 3.3 binary/TASTy baseline.
ThisBuild / scalaVersion := "3.3.8"
ThisBuild / crossScalaVersions := Seq("2.13.18", "3.3.8")
ThisBuild / tlJdkRelease := Some(17)
ThisBuild / tlFatalWarnings := true

// Typelevel's tagged-release convention, without snapshot publishing from main.
ThisBuild / tlCiReleaseBranches := Nil
ThisBuild / tlCiReleaseTags := ProjectInfo.isConfigured
ThisBuild / tlSitePublishBranch := None
ThisBuild / tlSitePublishTags := ProjectInfo.isConfigured
ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest")
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"), JavaSpec.temurin("21"))
ThisBuild / githubWorkflowPermissions := Some(Permissions.ReadAll)
ThisBuild / githubWorkflowIncludeClean := false
ThisBuild / githubWorkflowPublishNeeds ++= Seq("site", "coverage")
ThisBuild / githubWorkflowBuildPreamble += WorkflowStep.Use(
  UseRef.Public("actions", "setup-node", "v6"),
  Map("node-version" -> "24"),
  name = Some("Setup Node.js"),
  cond = Some("matrix.project == 'rootJS'")
)
ThisBuild / githubWorkflowBuildPreamble += WorkflowStep.Run(
  List("python3 scripts/check_repository.py"),
  name = Some("Check repository and API inventory")
)
ThisBuild / githubWorkflowBuild += WorkflowStep.Sbt(
  List("examples/compile"),
  name = Some("Compile examples"),
  cond = Some("matrix.project == 'rootJVM'")
)
ThisBuild / githubWorkflowAddedJobs += WorkflowJob(
  "coverage",
  "JVM Coverage",
  scalas = Nil,
  sbtStepPreamble = Nil,
  javas = List(JavaSpec.temurin("17")),
  oses = List("ubuntu-latest"),
  steps = githubWorkflowJobSetup.value.toList ++ List(
    WorkflowStep.Sbt(List("coverageJVM"), name = Some("Measure JVM test coverage")),
    WorkflowStep.Use(
      UseRef.Public("actions", "upload-artifact", "v4"),
      Map(
        "name" -> "jvm-coverage",
        "path" -> ".jvm/target/scala-*/scoverage-report",
        "if-no-files-found" -> "error",
        "include-hidden-files" -> "true"
      ),
      name = Some("Upload coverage report")
    )
  )
)
ThisBuild / githubWorkflowGeneratedCI ~= { jobs =>
  jobs.map {
    case job if Set("site", "dependency-submission").contains(job.id) =>
      job.withPermissions(
        Some(Permissions.Specify.defaultRestrictive.withContents(PermissionValue.Write))
      )
    case job => job
  }
}

// Safe-by-default local scaffold: publishing is skipped until metadata is set.
lazy val publishingSettings = Seq(
  publish / skip := !ProjectInfo.isConfigured
)

val catsVersion = "2.13.0"
val catsEffectVersion = "3.7.1"
val fs2Version = "3.12.2"
val munitVersion = "1.3.6"
val munitCatsEffectVersion = "2.2.0"
val disciplineMunitVersion = "2.0.0"
val disciplineVersion = "1.7.0"

lazy val root = tlCrossRootProject
  .aggregate(core, effect, streaming, all, laws, tests)
  .aggregate(docs, examples, unidocs)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .settings(publishingSettings)
  .settings(
    name := "pspsps-core",
    description := "Postfix Cats constructors for Scala 3",
    libraryDependencies += "org.typelevel" %%% "cats-core" % catsVersion
  )

// Independent of the core module. Import effect syntax alone when that is enough.
lazy val effect = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/effect"))
  .settings(publishingSettings)
  .settings(
    name := "pspsps-effect",
    description := "Postfix Cats Effect Resource and Ref constructors",
    libraryDependencies += "org.typelevel" %%% "cats-effect-kernel" % catsEffectVersion
  )

lazy val streaming = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/fs2"))
  .settings(publishingSettings)
  .settings(
    name := "pspsps-fs2",
    description := "Postfix FS2 Stream, Pull, and Chunk constructors",
    libraryDependencies += "co.fs2" %%% "fs2-core" % fs2Version
  )

lazy val all = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/all"))
  .settings(publishingSettings)
  .dependsOn(core, effect, streaming)
  .settings(
    name := "pspsps",
    description := "One import for pspsps constructor syntax"
  )

lazy val laws = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/laws"))
  .settings(publishingSettings)
  .dependsOn(core)
  .settings(
    name := "pspsps-laws",
    description := "Constructor-coherence laws and Discipline rules",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline-core" % disciplineVersion
    )
  )

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("tests"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(all, laws)
  .settings(
    name := "pspsps-tests",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.scalameta" %%% "munit-scalacheck" % "1.3.1" % Test,
      "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectVersion % Test,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion % Test,
      "org.typelevel" %%% "cats-effect-testkit" % catsEffectVersion % Test
    )
  )

lazy val examples = project
  .in(file("examples"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(all.jvm)
  .settings(
    name := "pspsps-examples",
    crossScalaVersions := Seq("3.3.8"),
    Compile / run / fork := true,
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion
  )

lazy val docs = project
  .in(file("docs"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(all.jvm, laws.jvm)
  .settings(
    name := "pspsps-docs",
    crossScalaVersions := Seq("3.3.8"),
    tlSiteIsTypelevelProject := None,
    tlSiteApiUrl := None,
    tlSiteHelium := tlSiteHelium.value.site.topNavigationBar(
      homeLink = ImageLink.internal(
        Path.Root / "index.md",
        Image.internal(Path.Root / "images" / "pspsps.svg", alt = Some("pspsps"))
      ),
      navLinks = Seq(IconLink.internal(Path.Root / "api" / "index.html", HeliumIcon.api))
    ),
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion,
    mdocIn := baseDirectory.value / "src" / "main" / "mdoc",
    mdocOut := target.value / "mdoc",
    // Laika validates the assembled site, including generated reference and source assets.
    mdocExtraArguments += "--no-link-hygiene",
    mdocVariables ++= Map(
      "ORGANIZATION" -> organization.value,
      "REPOSITORY_URL" -> ProjectInfo.url
    ),
    Laika / sourceDirectories += target.value / "site-reference",
    siteReference := {
      val apiDocs = (unidocs / Compile / doc).value
      val destination = target.value / "site-reference"
      IO.delete(destination)
      IO.createDirectory(destination)
      IO.copyDirectory(apiDocs, destination / "api")
      val reference = IO
        .read(baseDirectory.value / "reference.md")
        .replace("(../modules/", "(source/modules/")
        .replace(
          "[verification status](../VERIFICATION.md)",
          "[constructor semantics](semantics.md)"
        )
        .replace("(src/main/mdoc/index.md)", "(getting-started.md)")
      IO.write(destination / "reference.md", reference)
      IO.copyFile(baseDirectory.value / "design.md", destination / "design.md")
      val moduleDirectory = baseDirectory.value.getParentFile / "modules"
      Seq("core", "effect", "fs2", "all").foreach { module =>
        IO.copyDirectory(
          moduleDirectory / module / "src",
          destination / "source" / "modules" / module / "src"
        )
      }
    },
    laikaSite := laikaSite.dependsOn(siteReference).value,
    tlSitePreview := tlSitePreview.dependsOn(siteReference).value
  )

lazy val siteReference =
  taskKey[Unit](
    "Include unified Scaladoc, the generated API inventory and design notes in the site"
  )

lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(publishingSettings)
  .settings(
    name := "pspsps-docs",
    description := "Unified pspsps API documentation",
    crossScalaVersions := Seq("3.3.8"),
    ScalaUnidoc / unidoc / unidocProjectFilter :=
      inProjects(core.jvm, effect.jvm, streaming.jvm, all.jvm, laws.jvm),
    // Unpublished API URLs would otherwise replace links between these local source pages.
    ScalaUnidoc / unidoc / apiMappings ~= {
      _.filterNot { case (_, apiUrl) => apiUrl.getHost == "javadoc.invalid" }
    }
  )

// Explicit module commands also work without the generated GitHub workflows.
addCommandAlias("verifyJVM", ";+testsJVM/test;examples/compile;docs/tlSite")
addCommandAlias("verifyJS", ";+testsJS/test")
addCommandAlias("verify", ";verifyJVM;verifyJS")
addCommandAlias("fmt", ";+scalafmtAll;scalafmtSbt")
addCommandAlias("fmtCheck", ";+scalafmtCheckAll;scalafmtSbtCheck")
addCommandAlias(
  "coverageJVM",
  ";project rootJVM;coverage;test;coverageReport;coverageAggregate;coverageOff;project /"
)
addCommandAlias("ci", ";githubWorkflowCheck;+headerCheckAll;fmtCheck;verify")
