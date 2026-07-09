# falconer-android

[![](https://jitpack.io/v/alifhasnain/falconer-android.svg)](https://jitpack.io/#alifhasnain/falconer-android)

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

| Module | Coordinate (JitPack) | Contents |
|--------|----------------------|----------|
| `falconer-core` | `com.github.alifhasnain.falconer-android:falconer-core` | The seam: the `FalconerEngine` interface + the Dart↔Kotlin channel contract (`MethodNames` / `PayloadKeys`). Light deps only — coroutines-core + the Android SDK. |
| `falconer-impl` | `com.github.alifhasnain.falconer-android:falconer-impl` | The full inspector: Compose UI + `FalconerActivity`, SQLDelight DB + repository, notifications, export. Registers `RealFalconerEngine`. Carries the Activity / receiver / provider / `POST_NOTIFICATIONS` manifest entries. |
| `falconer-noop` | `com.github.alifhasnain.falconer-android:falconer-noop` | The empty mirror: `NoOpFalconerEngine` (every method inert). Depends on `falconer-core` and **nothing else** — no Compose, no SQLDelight, no manifest components. |

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

## Consuming (JitPack)

Add the JitPack repository, then the variant-scoped dependencies:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    api                   "com.github.alifhasnain.falconer-android:falconer-core:0.1.0"
    debugImplementation   "com.github.alifhasnain.falconer-android:falconer-impl:0.1.0"
    releaseImplementation "com.github.alifhasnain.falconer-android:falconer-noop:0.1.0"
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

## Publishing

JitPack builds from a Git tag. Push a tag matching `falconerVersion` in
`gradle.properties`, then trigger the build at
[jitpack.io/#alifhasnain/falconer-android](https://jitpack.io/#alifhasnain/falconer-android).
[`jitpack.yml`](jitpack.yml) pins JDK 17 and runs `publishToMavenLocal` for the
three library modules (the `:app` sample is never published).

> The Gradle `group` is `com.github.alifhasnain.falconer-android` — JitPack requires
> the `com.github.<user>.<repo>` form so inter-module POM dependencies resolve.
> Change it in [`build.gradle`](build.gradle) if your GitHub slug differs.

## Layout

```
falconer-android/
├── falconer-core/     FalconerEngine + channel contract        (published)
├── falconer-impl/     full inspector + RealFalconerEngine       (published, debug)
├── falconer-noop/     NoOpFalconerEngine                        (published, release)
├── app/               sample host + Method A verification       (not published)
├── jitpack.yml        JDK 17 + publish tasks
└── gradle.properties  falconerVersion — the single lockstep version
```

## License

Same as the parent Falconer plugin. See the
[Falconer repository](https://github.com/alifhasnain/falconer).
