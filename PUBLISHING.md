# Publishing to Maven Central

`io.transynq:transynq-sdk` is published to [Maven Central](https://central.sonatype.com/artifact/io.transynq/transynq-sdk)
through the **Sonatype Central Portal** by [`.github/workflows/release.yml`](.github/workflows/release.yml), using the
[`com.vanniktech.maven.publish`](https://github.com/vanniktech/gradle-maven-publish-plugin) Gradle plugin (pinned to 0.30.0 in
`build.gradle.kts`). It uploads the jar, sources jar, javadoc jar and POM, all GPG-signed, and releases the deployment.

Maven Central has no OIDC trusted publishing: the workflow authenticates with a Central Portal **user token** and signs with a
GPG key, both stored as GitHub secrets.

## One-time setup (repository owner)

1. **Central Portal account.** Sign in at <https://central.sonatype.com> (GitHub or e-mail sign-up).
2. **Verify the namespace `io.transynq`** (you own `transynq.io`). *Namespaces > Add Namespace* `io.transynq`; the portal shows a
   verification key. Add it as a **DNS TXT record on `transynq.io`** (host `@`, value = the key), wait for DNS to propagate, then
   press *Verify Namespace*. The TXT record can be removed once the namespace is verified.
3. **User token.** *Account > Generate User Token*. It shows a username and a password (not your login).
4. **GPG key** for signing:
   ```shell
   gpg --quick-generate-key "Transynq <release@transynq.io>" ed25519 sign 2y   # use an address you control
   gpg --list-secret-keys --keyid-format=long                                   # note the key id
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>                    # Central verifies signatures against public keyservers
   gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID> > signing-key.asc                  # for the secret below; delete the file afterwards
   ```
5. **GitHub environment.** Repository *Settings > Environments > New environment* `release`. Optionally add **required
   reviewers** (manual approval before every publish) and restrict it to tags `v*`.
6. **Secrets** (environment secrets of `release`, or repository secrets):

   | Secret | Value |
   |---|---|
   | `MAVEN_CENTRAL_USERNAME` | user token username (step 3) |
   | `MAVEN_CENTRAL_PASSWORD` | user token password (step 3) |
   | `SIGNING_KEY` | the whole ASCII-armoured private key (`signing-key.asc`, including the BEGIN/END lines) |
   | `SIGNING_PASSWORD` | the key's passphrase (empty if none) |

Without these secrets nothing is signed or uploaded: `./gradlew build` and `./gradlew publishToMavenLocal` work anywhere with no
secrets.

## Release procedure

1. Update `version` in `build.gradle.kts` and `SdkInfo.VERSION` in `src/main/java/io/transynq/sdk/SdkInfo.java` (semantic
   versioning; the SDK version is independent of the API version).
2. Move the "Unreleased" entries of [CHANGELOG.md](CHANGELOG.md) under a new `## [X.Y.Z] - YYYY-MM-DD` heading.
3. Open a pull request, let CI pass, merge to `main`.
4. Create a GitHub release with a **new tag `vX.Y.Z`** (exactly the library version with a `v` prefix) on `main`, paste the
   changelog section as notes, and publish it.
5. The `release` workflow checks the tag against `build.gradle.kts` (it fails on a mismatch), runs `./gradlew build javadoc`
   and `publishToMavenLocal`, then `./gradlew publishAndReleaseToMavenCentral` (after the `release` environment approval, if
   configured). The Central Portal validates the deployment (signatures, POM, javadoc and sources) and publishes it; it is
   searchable on Maven Central typically within 30 minutes.

**Dry run:** *Actions > release > Run workflow* with `dry-run` checked (the default) builds, tests and publishes unsigned to the
runner's local Maven repository only.

Maven Central releases are permanent: a version can never be changed or deleted. Fix mistakes with a new version.
