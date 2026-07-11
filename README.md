# Folia Addon Template

A standalone starter for building an **addon** for a FoliaTemplate-based host plugin. Your addon compiles against the host's published `addon-api`, builds to a single jar, and drops into the host's `addons/` folder — no need to clone or build the host itself.

```bash
./gradlew build         # → build/libs/MyAddon-1.0.0.jar
./gradlew deployAddon   # builds + copies it into your test server (see below)
```

---

## Quick start

1. **Requirements:** JDK 21. (The wrapper handles Gradle.)

2. **Authenticate to GitHub Packages.** GitHub needs a token even for *public* packages. The build finds one automatically if you already have GitHub auth on your machine, so usually there's **nothing to do**:

   | | Where it looks | Set up by |
   |---|---|---|
   | 1 | `gpr.user` / `gpr.token` in `~/.gradle/gradle.properties` | you, manually |

   Rows 3–4 mean that if your IDE can already talk to GitHub, the build picks up that token with zero configuration. Check what it sees:
   ```bash
   ./gradlew whoAmI
   ```
   Nothing found? Either run `gh auth login`, or create a [token](https://github.com/settings/tokens) with **only** the `read:packages` scope and add it to your **global** `~/.gradle/gradle.properties` (never commit it):
   ```properties
   gpr.user=your-github-username
   gpr.token=ghp_xxxxxxxxxxxxxxxx
   ```

   > Caveat: no build tool can read IntelliJ's *own* credential store (PasswordSafe). What works is Git's credential helper, which IntelliJ's GitHub sign-in normally populates — that's what row 4 reads.

3. **Point it at the host.** In `gradle.properties`:
   ```properties
   hostRepo=YourName/folia-template     # the host's GitHub repo (owner/repo)
   hostGroup=com.example                # the host's Maven group
   hostApiVersion=1.0.0                 # the API version to build against
   ```
   *Building against a host you compiled yourself?* Run `./gradlew publishApiLocally` in the host project instead — this template already lists `mavenLocal()`, so it'll resolve with no token needed.

4. **Rename things:** `addonName`/`group`/`version` in `gradle.properties`, the package under `src/main/kotlin/`, and `name`/`main` in `src/main/resources/addon.yml`.

5. **Build and install:** drop `build/libs/MyAddon-1.0.0.jar` into `plugins/<Host>/addons/`, then start the server (or run `/addons reload`).

6. **Verify:** the console should log `Enabled addon MyAddon v1.0.0`, and `/addons` should list it.

**Faster loop:** set `testServerPath` in `gradle.properties`, then `./gradlew deployAddon` builds and copies the jar into the server's addons folder in one step.

**CI:** `build.yml` builds on every push; `release.yml` ships a GitHub Release when you push a `v*` tag.

---

## The golden rule: never shade the API

```kotlin
compileOnly("com.example:folia-template-addon-api:1.0.0")   // ✅
implementation("com.example:folia-template-addon-api:1.0.0") // ❌ breaks loading
```

The host loads your addon with a classloader whose **parent is the host's**, so `Addon`, `Menu`, `AddonContext` etc. resolve to the host's class objects. If you bundle your own copy, you get a *second class with the same name*, `isAssignableFrom(Addon)` fails, and the host refuses to load your addon — with an error that looks nothing like the actual cause.

Same goes for the Folia API and the Kotlin stdlib: both are provided at runtime, so both are `compileOnly`.

**Your own libraries are different** — those you *do* bundle (`implementation`) and should relocate in `shadowJar`, so two addons shipping different versions of the same library can't collide.

---

## What's in the box

| File | What it shows |
|---|---|
| `MyAddon.kt` | Entry point — the lifecycle and all four registration types |
| `MyCommand.kt` | An addon command, using the host's command framework |
| `MyListener.kt` | A Bukkit listener (auto-unregistered on unload) |
| `MyAddonConfig.kt` | Your own `config.yml` in the addon's private data folder |
| `GreetingService.kt` | A service other addons can consume |
| `addon.yml` | The manifest the host reads from your jar |

### The lifecycle

```kotlin
class MyAddon : AddonBase() {
    override fun onEnable() {
        context.registerCommand(MyCommand(this))    // host's @Command annotations
        context.registerListener(MyListener(this))  // auto-unregistered on unload
        context.registerService<GreetingService>(impl)   // other addons can use this
        context.schedulers.async { /* ... */ }       // auto-cancelled on unload
        context.logger.info("Ready")                // prefixed "[MyAddon]"
        // context.dataFolder → plugins/<Host>/addons/MyAddon/
    }
    override fun onDisable() { /* only for state the host can't see */ }
}
```

Everything registered through `context` is tracked per-addon and undone on unload, so `onDisable` is usually empty. That cleanup matters on Folia: a leaked repeating task would keep running against a dead classloader after a reload.

### addon.yml

```yaml
name: MyAddon
version: 1.0.0
main: com.example.myaddon.MyAddon
api-version: 1              # must match the host's ADDON_API_VERSION
depends: [ OtherAddon ]     # optional — those load first
```

If `api-version` doesn't match the host, it refuses to load your addon and says so — deliberately, instead of letting it crash later with a confusing `NoSuchMethodError`. Missing `depends` and dependency cycles are also reported and skipped.

---

## Folia threading (the part that bites people)

**There is no main thread.** Each region ticks on its own thread. Never touch `Bukkit.getScheduler()` — use `context.schedulers`:

| Helper | Runs on | Use for |
|---|---|---|
| `global { }` | global region | world-wide state, your addon's shared data |
| `region(loc) { }` | region owning `loc` | blocks/world at a location |
| `entity(e) { }` | region owning `e` | a player/entity, opening inventories |
| `async { }` | background thread | HTTP/DB/disk — never world state |

Command and event handlers already run on the region owning the relevant player/block, so touching *that* player there is safe. Two things that are **not** automatically safe:

- **Your own shared state** (fields, maps, caches) — different regions run handlers in parallel. Use `ConcurrentHashMap`/atomics, or confine the state to one thread (usually global).
- **Anything after you go async** — hop back onto an owning region before touching the game again.

---

## CI

- **`build.yml`** — builds and uploads the jar on every push/PR.
- **`release.yml`** — publishes a GitHub Release with your jar when you push a `v*` tag.

Both authenticate to GitHub Packages with the built-in `GITHUB_TOKEN`, which works when the host's package is in the same GitHub org/user as your addon. (If it isn't, add a PAT with `read:packages` as a repo secret and use it for `GITHUB_TOKEN` in the workflow env.)
