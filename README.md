# AppleSkinOML

[AppleSkin](https://github.com/squeek502/AppleSkin) (by squeek502, Unlicense) ported to
[OML](https://github.com/OhMyLoader/OhMyLoader) — the food/hunger HUD overlays, the
server→client sync of saturation, exhaustion and the natural-regeneration rule, and the F3
food-stats debug line. Source of record for the port is the `26.3-fabric` branch; this is the
AC-6 migration measure of the loader's development plan.

## Running it

```sh
gradlew runClient        # a normal OML dev client; build with `gradlew build`
```

The project consumes `org.ohmyloader:*` and the `org.ohmyloader.gradle` plugin from Maven only
(`oml_version` in `gradle.properties`); publish the loader with `gradlew publishToMavenLocal`
first, like any third-party mod project.

## What changed relative to the Fabric branch

| Fabric surface | OML replacement |
|---|---|
| `main` + `client` entrypoints | one `@Mod` class; side-specific work rides the side's own events |
| `PayloadTypeRegistry` + `ServerPlayNetworking` / `ClientPlayNetworking` | `OMLPayloadType` + `OMLNetwork.sendToPlayer` + `serverToClient` receivers; the main-thread hop is `OMLScheduler.client.schedule` |
| `ServerPlayerMixin` (per-player tick) | `Events.LEVEL_TICK` iterating the level's players — same site, same cadence, no mixin |
| `PlayerListMixin` (`placeNewPlayer` tail) | `Events.PLAYER_JOINED` — the OML event is anchored at that exact tail |
| `MinecraftMixin` (per-tick alpha flash) | `Events.CLIENT_TICK` |
| `HudMixin` (`extractFood` / `extractHearts` geometry) | **stays a mixin** — OML's `@Mixin`/`@Inject` with the 26.3 descriptors pinned and `require = 1`; no OML event carries the bar coordinates |
| `FoodDataMixin` (`@Shadow` + manipulator interface) | an `@Accessor` interface mixin synthesizing getters/setters for the private `exhaustionLevel` |
| jankson config + cloth-config screens | the loader's TOML config (`config/appleskin.toml`); entries and comments are the upstream ones. **No config GUI** — the loader has no screen framework for it yet |
| fabric-api `Event<T>` behind the `AppleSkinApi` events | a small array-backed listener list with the same `EventHandler` shape |
| REI / JEI / ModMenu integrations | **not ported** — other-loader interop |
| tooltip overlay (`TooltipOverlayHandler` + `ItemStackMixin` + `ClientTooltipComponentMixin`) | **not ported yet** — the tooltip hook rides interface-static mixins, the most exotic shape in the mod; planned as the next batch |

Assets (textures, lang files) are carried over unchanged, `pack.mcmeta` included.

## Status

- [x] entry, config, network sync, server tracking (event-anchored, mixin-free)
- [x] HUD overlays (saturation / exhaustion / held-food hunger+saturation / estimated health)
- [ ] F3 food-stats debug entry — blocked: 26.3's `DebugScreenEntries.register` is private and OML's
  accessor synthesis covers instance members only (the upstream branch calls the same private method)
- [ ] tooltip overlay (food values on item tooltips)
- [x] real-machine runs: a dedicated server boots with the mod loaded (config generated, mixin
  self-check green), and a client joined a world for a 60s in-window run — the sync path
  (the accessor cast, every server tick) and the HUD hooks (every frame) ran clean
- [x] **HUD visually confirmed in-game** (2026-11-07): the hunger bar overlays — saturation,
  held-food restore, exhaustion underlay — render correctly on the real client

## Findings fed back to the loader (AC-6)

1. **`@Accessor` synthesis did not declare the mixin interface on the target class** — the methods
   existed, but a mod's typed cast threw `ClassCastException` on the first in-world tick. Fixed in
   the loader (`ClassMerger`, with a test); this port is the reason it was found.
2. **Handler capture parameters require an explicit `LocalCapture`** — the mixin self-check rejects
   a capturing handler without `locals = LocalCapture.CAPTURE_FAILHARD` at startup, by name.
3. **Injection descriptors are pinned exactly** — a one-character drift in `extractHearts`'s
   descriptor produced a startup `InjectionError` naming the actual shape; the client-only crash
   surfaced what a server boot cannot (the server never loads `Hud`).
4. The Zulu-27 window-creation `0xC0000005` (known; the build file carries the
   `-XX:TieredStopAtLevel=1` workaround the verification mod uses).
