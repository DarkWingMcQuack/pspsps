# Releasing pspsps

pspsps is currently unreleased. The build provides Typelevel versioning,
compatibility tooling, generated GitHub Actions, and a checked microsite with
embedded unified Scaladoc.

## Repository setup

The committed metadata targets [DarkWingMcQuack/pspsps](https://github.com/DarkWingMcQuack/pspsps)
and Maven namespace `io.github.darkwingmcquack`. Check the namespace ownership,
maintainer identity, and remote in [project/ProjectInfo.scala](project/ProjectInfo.scala)
before the first publication.

The build is configured for **tagged releases**: it does not publish snapshots
from main. Artifact and microsite publication use the generated workflow's
release conditions. Local `PSPSPS_GITHUB_OWNER`, `PSPSPS_REPOSITORY`,
`PSPSPS_ORGANIZATION`, and `PSPSPS_MAINTAINER` overrides are available; permanent
metadata changes belong in the committed defaults and regenerated workflows.

Follow the current [sbt-typelevel secrets guide](https://typelevel.org/sbt-typelevel/secrets.html)
for Maven Central/Sonatype namespace verification and signing. Configure
`SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `PGP_SECRET`, and, if needed,
`PGP_PASSPHRASE` as repository secrets. Use account tokens where required.

Enable private vulnerability reporting and document a maintainer contact
channel in [SECURITY.md](SECURITY.md).

## Validate the candidate

Use the pinned toolchain with `nix develop path:.`, then:

```sh
sbt prePR
sbt verify
python3 scripts/api_inventory.py
python3 scripts/check_repository.py
sbt 'show allJVM/version'
```

`verify` covers Scala 2.13 and Scala 3 on JVM and Scala.js, the example, and the
microsite with checked Scala 3 examples and unified Scaladoc. Inspect dependency
evictions, compiler contracts, rendered documentation, and the complete diff.
Update [CHANGELOG.md](CHANGELOG.md) and record executed results in
[VERIFICATION.md](VERIFICATION.md).

Run MiMa checks once a published baseline exists. There is no baseline for the
initial release. Binary checks supplement source-level inference contracts;
they cannot establish overload source compatibility alone.

Run `sbt githubWorkflowGenerate` when changing workflow settings and inspect
the generated diff. Preserve platform, examples, documentation, and coverage
checks. Publishing secrets must remain outside untrusted pull-request jobs.

## Publish the release

The version is Git-derived; `tlBaseVersion` sets the intended series. Create
the selected `v0.1.x` tag only after the candidate passes. Push the tag
deliberately and monitor the generated publishing jobs.

Resolve every published artifact from a fresh consumer project. Check POM
metadata, source jars, Scaladoc jars, and signatures, then compile the advertised
usage examples. Confirm both Scala binary versions on JVM and Scala.js. Record
actual versions and platform results in the release notes.

## Deploy the microsite

The generated Typelevel site workflow publishes the microsite to `gh-pages`
on release tags. Set GitHub Pages to the root of that branch and review the
publishing job's repository permissions. The expected site address is
`https://darkwingmcquack.github.io/pspsps/` once the first deployment completes.

Preview locally with `sbt 'docs/tlSitePreview'` or generate the static site with
`sbt 'docs/tlSite'`. The output includes the API documentation under `api/`.
Check installation coordinates and API navigation after the first release.
See the [official site guide](https://typelevel.org/sbt-typelevel/site.html) for
current deployment requirements.

## Publish locally

Run `sbt publishLocal` and use the exact output of `sbt 'show allJVM/version'`
as a consumer's dependency version. Local publication is separate from Maven
Central publication and site deployment.
