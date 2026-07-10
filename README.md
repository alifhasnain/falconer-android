# falconer-android

[![Maven Central](https://img.shields.io/maven-central/v/io.github.alifhasnain/falconer-core)](https://central.sonatype.com/artifact/io.github.alifhasnain/falconer-core)

Native Android artifacts for [**Falconer**](https://github.com/alifhasnain/falconer) —
a Chucker-style HTTP inspector for the Dio client on Flutter.

This repo exists for one reason: to make Falconer's inspector — its Compose UI,
SQLDelight database, notification stack, and manifest components — **physically
absent from a consumer's release build**, automatically, per build type, the way
Chucker's `library-no-op` works. It is the "Method A" implementation described in
the plugin's `RELEASE_STRIPPING.md`.

> **Why this matters (PCI / payment context):** captured HTTP payloads can contain
> cardholder data, tokens, and credentials. A release build that merely *depends*
> on the inspector still opens its on-device database at startup. These artifacts
> make the inspector absent — not merely runtime-disabled — in any build that can
> reach cardholder data.

---

## The three artifacts

| Module | Coordinate (Maven Central) | Contents |
|--------|----------------------------|----------|
| `falconer-core` | `io.github.alifhasnain:falconer-core` | The seam: the `FalconerEngine` interface + the Dart↔Kotlin channel contract (`MethodNames` / `PayloadKeys`). Light deps only — coroutines-core + the Android SDK. |
| `falconer-impl` | `io.github.alifhasnain:falconer-impl` | The full inspector: Compose UI + `FalconerActivity`, SQLDelight DB + repository, notifications, export. Registers `RealFalconerEngine`. Carries the Activity / receiver / provider / `POST_NOTIFICATIONS` manifest entries. |
| `falconer-noop` | `io.github.alifhasnain:falconer-noop` | The empty mirror: `NoOpFalconerEngine` (every method inert). Depends on `falconer-core` and **nothing else** — no Compose, no SQLDelight, no manifest components. |

All three ship on **one version, released in lockstep** — the no-op's
`FalconerEngine` surface must always match core.

## How it works

The thin plugin talks to the implementation only through `FalconerEngine`
(in `falconer-core`) and discovers the concrete engine at runtime via
`ServiceLoader`. The consumer wires the two implementations to build-type-scoped
configurations:

```
       ┌───────────────────────────────┐
       │ FalconerEngine  (falconer-core)│   ← the only type the caller references
       └───────────────┬───────────────┘
        implements      │      implements
   ┌────────────────────┴─────────────────────┐
   │                                           │
RealFalconerEngine (falconer-impl)      NoOpFalconerEngine (falconer-noop)
  Compose · SQLDelight · UI · notif        empty stubs, zero heavy deps
        ▲ debug only                            ▲ release only
```

```gradle
debugImplementation   "…:falconer-impl:<version>"   // ServiceLoader → RealFalconerEngine
releaseImplementation "…:falconer-noop:<version>"   // ServiceLoader → NoOpFalconerEngine
```

In a **release** build only `falconer-noop` is on the classpath, so nothing
references Compose, SQLDelight, `FalconerActivity`, or the notification stack —
R8 has no live edge to them, and the no-op contributes no manifest components.
The inspector is gone from the bytes. In **debug**, `falconer-impl` is linked and
everything works.

## Consuming (Maven Central)

Artifacts are on Maven Central, so `mavenCentral()` (already in most projects) is
the only repository needed:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    api                   "io.github.alifhasnain:falconer-core:0.1.0"
    debugImplementation   "io.github.alifhasnain:falconer-impl:0.1.0"
    releaseImplementation "io.github.alifhasnain:falconer-noop:0.1.0"
}
```

> **Consumer note — pin the Kotlin stdlib.** `falconer-impl` pulls SQLDelight,
> which transitively drags in a newer `kotlin-stdlib` than the pinned Kotlin
> 2.1.0 compiler. On AGP 8.9.1 the 2.2 metadata jams R8/D8. Any consumer that
> links the impl (i.e. the debug variant) must force the stdlib back down — see
> the `configurations.all { resolutionStrategy { force … } }` block in
> [`app/build.gradle`](app/build.gradle) for the exact rule.

The Flutter plugin (published on pub.dev) does this wiring internally, so app
authors keep the one-line pub install.

## Running the sample

The three modules are libraries — nothing to run directly. The **`:app`** module
is a Flutter-free host that consumes them exactly as the plugin does (only via
`FalconerEngine` + `ServiceLoader`), so it doubles as the Method A verification
harness. Its status line prints which engine loaded.

```bash
# On a device/emulator — see the real inspector UI:
./gradlew :app:installDebug
#   open "Falconer Sample" → Inject sample traffic → Open inspector

# Headless — compile only:
./gradlew :app:assembleDebug
```

## Verifying the stripping

Stripping is a claim about the binary — prove it, don't assume it.

```bash
./gradlew :app:assembleRelease
```

The R8-minified release APK (`app/build/outputs/apk/release/`) contains **no**
`androidx/compose/**`, **no** `app/cash/sqldelight/**`, **no**
`dev/alifhasnain/falconer/ui/**`, and **no** `FalconerActivity` — only
`NoOpFalconerEngine`. The merged release manifest declares none of the inspector
components. The debug APK contains all of them. Confirm with Android Studio's
APK Analyzer, or dump `classes*.dex` and grep the type descriptors.

## Build & test

```bash
./gradlew :falconer-impl:testDebugUnitTest      # pure-JVM unit tests (real SQLite via JDBC)
./gradlew :falconer-impl:assembleDebugAndroidTest   # compile instrumented tests (run needs an emulator)
./gradlew :falconer-core:assembleRelease :falconer-noop:assembleRelease :falconer-impl:assembleDebug
```

Pinned toolchain: Kotlin 2.1.0 · AGP 8.9.1 · Gradle 8.12 · SQLDelight 2.2.1 ·
compileSdk/targetSdk 36 · minSdk 21 · jvmTarget 17.

## Publishing (Maven Central)

Publishing uses the [vanniktech `maven-publish`](https://github.com/vanniktech/gradle-maven-publish-plugin)
plugin, driven by the `GROUP` / `VERSION_NAME` / `POM_*` / `SONATYPE_*` properties
in `gradle.properties`. It builds the signed `.aar` + `-sources.jar` +
`-javadoc.jar` + POM for each of the three library modules and uploads them to the
Sonatype **Central Portal** (`SONATYPE_HOST=CENTRAL_PORTAL`). The `:app` sample is
never published.

One-time setup:

1. **Verify the namespace** `io.github.alifhasnain` at
   [central.sonatype.com](https://central.sonatype.com) (GitHub-ownership check —
   no domain required). If your GitHub user differs, change `GROUP` in
   `gradle.properties`.
2. **GPG key** — generate one, publish the public key to a keyserver.
3. **Credentials** — put these in `~/.gradle/gradle.properties` (NOT in the repo):

   ```properties
   mavenCentralUsername=<Central Portal token user>
   mavenCentralPassword=<Central Portal token>
   signing.keyId=<last 8 of GPG key id>
   signing.password=<GPG key passphrase>
   signing.secretKeyRingFile=<path to secring.gpg>
   ```

Then release the whole version in lockstep:

```bash
./gradlew publishAllPublicationsToMavenCentralRepository
```

Sanity-check the artifacts/POMs locally first, without keys:

```bash
./gradlew publishToMavenLocal -PRELEASE_SIGNING_ENABLED=false
```

## Layout

```
falconer-android/
├── falconer-core/     FalconerEngine + channel contract        (published)
├── falconer-impl/     full inspector + RealFalconerEngine       (published, debug)
├── falconer-noop/     NoOpFalconerEngine                        (published, release)
├── app/               sample host + Method A verification       (not published)
└── gradle.properties  GROUP / VERSION_NAME + POM/signing config (lockstep version)
```

## License

MIT — see [`LICENSE`](LICENSE). Same license as the parent
[Falconer plugin](https://github.com/alifhasnain/falconer).
