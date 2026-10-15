import laika.ast.{Image, Path}
import laika.helium.config.{
  ButtonLink,
  HeliumIcon,
  IconLink,
  ImageLink,
  LinkPanel,
  Teaser,
  TextLink
}
import org.typelevel.sbt.gha.{Permissions, PermissionValue}

val repositoryUrl = "https://github.com/DarkWingMcQuack/pspsps"

ThisBuild / tlBaseVersion := "0.1"
ThisBuild / organization := "io.github.darkwingmcquack"
ThisBuild / organizationName := "pspsps contributors"
ThisBuild / startYear := Some(2026)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / homepage := Some(url("https://darkwingmcquack.github.io/pspsps/"))
ThisBuild / scmInfo := Some(
  ScmInfo(url(repositoryUrl), "scm:git:" + repositoryUrl + ".git")
)
ThisBuild / developers := List(tlGitHubDev("DarkWingMcQuack", "pspsps contributors"))
ThisBuild / versionScheme := Some("early-semver")

// Deliberately retain the Scala 3.3 binary/TASTy baseline.
ThisBuild / scalaVersion := "3.3.8"
ThisBuild / crossScalaVersions := Seq("2.13.18", "3.3.8")
ThisBuild / tlJdkRelease := Some(17)
ThisBuild / tlFatalWarnings := true

// Publish artifacts from release tags and keep the documentation current on master.
ThisBuild / tlCiReleaseBranches := Nil
ThisBuild / tlCiReleaseTags := true
ThisBuild / tlSitePublishBranch := Some("master")
ThisBuild / tlSitePublishTags := false
ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest")
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"), JavaSpec.temurin("21"))
ThisBuild / githubWorkflowPermissions := Some(Permissions.ReadAll)
ThisBuild / githubWorkflowIncludeClean := false
ThisBuild / githubWorkflowPublishNeeds += "site"
ThisBuild / githubWorkflowBuildPreamble += WorkflowStep.Use(
  UseRef.Public("actions", "setup-node", "v6"),
  Map("node-version" -> "24"),
  name = Some("Setup Node.js"),
  cond = Some("matrix.project == 'rootJS'")
)
ThisBuild / githubWorkflowBuild += WorkflowStep.Sbt(
  List("examples/compile"),
  name = Some("Compile examples"),
  cond = Some("matrix.project == 'rootJVM'")
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
  .settings(
    name := "pspsps-core",
    description := "Postfix Cats constructors",
    libraryDependencies += "org.typelevel" %%% "cats-core" % catsVersion
  )

// Independent of the core module. Import effect syntax alone when that is enough.
lazy val effect = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/effect"))
  .settings(
    name := "pspsps-effect",
    description := "Postfix Cats Effect Resource and Ref constructors",
    libraryDependencies += "org.typelevel" %%% "cats-effect-kernel" % catsEffectVersion
  )

lazy val streaming = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/fs2"))
  .settings(
    name := "pspsps-fs2",
    description := "Postfix FS2 Stream, Pull, and Chunk constructors",
    libraryDependencies += "co.fs2" %%% "fs2-core" % fs2Version
  )

lazy val all = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/all"))
  .dependsOn(core, effect, streaming)
  .settings(
    name := "pspsps",
    description := "One import for pspsps constructor syntax"
  )

lazy val laws = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/laws"))
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
    tlSiteHelium := tlSiteHelium.value.site
      .landingPage(
        logo = Some(Image.internal(Path.Root / "images" / "cat.svg", alt = Some("pspsps cat"))),
        title = Some("pspsps"),
        subtitle = Some("Keep your Scala pipelines flowing."),
        license = Some("Apache 2.0"),
        titleLinks = Seq(
          ButtonLink.internal(Path.Root / "getting-started.md", "Get started"),
          ButtonLink.internal(Path.Root / "api" / "index.html", "Browse the API")
        ),
        linkPanel = Some(
          LinkPanel(
            "Documentation",
            TextLink.internal(Path.Root / "modules.md", "Modules and imports"),
            TextLink.internal(Path.Root / "conditionals.md", "Conditional builders"),
            TextLink.internal(Path.Root / "semantics.md", "Evaluation and resource safety"),
            TextLink.internal(Path.Root / "api.md", "API documentation")
          )
        ),
        projectLinks = Seq(IconLink.external(repositoryUrl, HeliumIcon.github)),
        teasers = Seq(
          Teaser(
            "Read left to right",
            "Construct transformers, resources, and streams where the value already is."
          ),
          Teaser(
            "Keep familiar types",
            "Return normal Cats, Cats Effect, and FS2 types with their existing semantics."
          ),
          Teaser(
            "Choose your modules",
            "Use Cats syntax alone, add effect or streaming syntax, or bring them together with one import."
          )
        )
      )
      .site
      .topNavigationBar(
        homeLink = ImageLink.internal(
          Path.Root / "README",
          Image.internal(Path.Root / "images" / "pspsps.svg", alt = Some("pspsps"))
        ),
        navLinks = Seq(ButtonLink.internal(Path.Root / "api" / "index.html", "API"))
      ),
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion,
    mdocIn := baseDirectory.value / "src" / "main" / "mdoc",
    mdocOut := target.value / "mdoc",
    // Scaladoc joins the site at the Laika stage, which validates the assembled links.
    mdocExtraArguments += "--no-link-hygiene",
    Laika / sourceDirectories += target.value / "site-api",
    siteApi := {
      val apiDocs = (unidocs / Compile / doc).value
      val destination = target.value / "site-api"
      IO.delete(destination)
      IO.createDirectory(destination)
      IO.copyDirectory(apiDocs, destination / "api")
    },
    laikaSite := laikaSite.dependsOn(siteApi).value,
    tlSitePreview := tlSitePreview.dependsOn(siteApi).value
  )

lazy val siteApi = taskKey[Unit]("Include unified Scaladoc in the microsite")

lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(
    name := "pspsps-docs",
    description := "Unified pspsps API documentation",
    crossScalaVersions := Seq("3.3.8"),
    ScalaUnidoc / unidoc / unidocProjectFilter :=
      inProjects(core.jvm, effect.jvm, streaming.jvm, all.jvm, laws.jvm),
    ScalaUnidoc / unidoc / scalacOptions ++= Seq(
      "-project-logo",
      ((LocalRootProject / baseDirectory).value / "docs/src/main/mdoc/images/cat.svg").getAbsolutePath,
      "-doc-root-content",
      ((LocalRootProject / baseDirectory).value / "docs/src/main/scaladoc/index.md").getAbsolutePath
    ),
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
addCommandAlias("ci", ";githubWorkflowCheck;+headerCheckAll;fmtCheck;verify")
