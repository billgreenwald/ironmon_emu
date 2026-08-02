For every feature that is built, your most common mistake is not looking at the original ironmon tracker lua scripts.  This is ESPECIALLY TRUE for memory addresses.  For every feature, before you make any edits or start to develop, you MUST review the code in the lua script and make sure you are keeping parity and copying their constants.  if you think that you should stray, ask me before you go ahead and do.

Also this code is generalizable across 5 games; do not hard code the pokemon emerald codes in to any of the scripts you write

The maxData/*.json files (max-fr-gen4.json, max-fr.json, max-fr-gen5-fr.json, max-em.json) have GameInfo fields like "GameName": "Pokemon Emerald (U)" that do NOT reflect the actual base ROM. These names are meaningless metadata — the JSON address values are the source of truth. Do not second-guess or "fix" the GameInfo names.

## KMP: logic-vs-render split (READ BEFORE ADDING ANY TRACKER FEATURE)

The tracker is being shared across Android and (upcoming) iOS via a Kotlin Multiplatform module,
`mgba-android-memapi/android-fork/tracker-core`. To avoid breaking one platform while building for
the other, every change must respect this split:

- **Shared LOGIC → `:tracker-core/src/commonMain`.** Memory decoding, per-game addresses, tables,
  readers, persistence models/logic, and the poll/battle/route/game-over logic. Write it once here.
- **Platform RENDER + glue → `:app` (and later iosMain/SwiftUI).** Compose UI (`TrackerPanel`,
  overlays), JNI, Activity lifecycle, SAF (`LogFileLocator`), service IPC (`QuickloadManager`).
  UI is a **dumb renderer of `TrackerState`** — put NO business logic in the UI layer, so both
  platforms share the logic and only re-skin the surface.

Decide first for any new feature: is it logic (→ commonMain) or render (→ per-platform UI)?
Logic goes in once; UI is written per platform.

### commonMain must stay platform-clean — do NOT use in commonMain:
- `android.*`, `androidx.*`, Compose, Gson, or any `java.*` API.
- JVM-only stdlib that silently compiles for Android but breaks iOS. Use the common equivalents:
  - `@Volatile` → `import kotlin.concurrent.Volatile`
  - `String(bytes, Charsets.X)` / `Charsets` → `bytes.decodeToString()` or a byte→char map
  - `java.util.concurrent.atomic.*` → `kotlinx.atomicfu.atomic(...)`
  - `System.currentTimeMillis()` → `nowMillis()` (kotlinx-datetime)
  - `java.io.File` path parsing → `String.substringAfterLast('/')` etc.
  - serialization → `kotlinx.serialization` (`trackerJson`), never Gson

### Platform capabilities go through SEAMS (interfaces), injected — never `Context` in common
Existing seams in `commonMain/.../tracker/platform/`: `Logger`/`TrackerLog`, `FileStore`,
`KeyValueStore`, `AssetReader`, `LogSource`, `TrackerSettings`, bundled as `TrackerEnvironment`
(passed into `TrackerPoller.start`). Need a new platform ability? Add an interface here, implement
it in `:app` (`tracker/platform/AndroidPlatform.kt`) and later in iosMain. Never reach for
`Context`/SharedPreferences/SAF from commonMain.

### ALWAYS validate common code against the iOS proxy before assuming it's safe
The plain Android/JVM build will NOT catch iOS-incompatible code. Before finishing a commonMain
change, run (from `android-fork/`, with the usual `ANDROID_HOME=... gradle` prefix):
- `:tracker-core:compileKotlinLinuxX64`  (compiles commonMain through Kotlin/Native ON LINUX — the iOS gate)
- `:tracker-core:linuxX64Test` and `:tracker-core:jvmTest`

### Persistence wire format is load-bearing
Keep `@SerialName` values and field defaults stable so existing device saves keep loading. Only
add fields as defaulted/optional. See `commonTest/.../SerializationTest.kt`.

See `CLAUDE_ARCHITECTURE.md` and the `.claude` memory `kmp_tracker_core.md` for the full layout.
