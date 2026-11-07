package squeek.appleskin

import org.ohmyloader.api.Mod
import org.ohmyloader.api.ModContext
import org.ohmyloader.api.OMLModInitializer
import org.ohmyloader.api.network.OMLNetworkProvider
import org.ohmyloader.api.network.OMLNetworkRegistry
import org.ohmyloader.api26_3.event.Events
import squeek.appleskin.client.HUDOverlayHandler
import squeek.appleskin.network.SyncHandler
import squeek.appleskin.network.SyncPayloads

/**
 * AppleSkin on OML. Ported from the 26.3-fabric branch; the differences are exactly where the
 * Fabric API surface meets OML's:
 *
 * - entrypoints (`main` / `client`) → one `@Mod` class; side-specific work rides the side's own
 *   events, which only fire on the side that owns them
 * - `ServerPlayerMixin.tick` / `PlayerListMixin.placeNewPlayer` / `MinecraftMixin.tick` → the OML
 *   events `LEVEL_TICK` / `PLAYER_JOINED` / `CLIENT_TICK` anchored at the same game sites
 * - `PayloadTypeRegistry` + `ServerPlayNetworking` / `ClientPlayNetworking` → `OMLNetwork`
 * - jankson config + cloth-config screens → the loader's TOML config (no GUI — see README)
 * - `HudMixin` and the `FoodData` internals stay mixins: no OML event carries the HUD bar
 *   coordinates, and exhaustion is a private field
 *
 * Not ported (see README): the REI/JEI/ModMenu integrations and the tooltip overlay.
 */
@Mod(id = "appleskin", name = "AppleSkin", version = "0.1.0")
class AppleSkinMod : OMLModInitializer, OMLNetworkProvider {

    override fun onInitialize(context: ModContext) {
        ModConfig.init(context.config)
        SyncHandler.init()
        HUDOverlayHandler.init()
        // The flash alpha drives the held-food overlays; only the client ticks it.
        Events.CLIENT_TICK.register { HUDOverlayHandler.onClientTick() }
        // The F3 food-stats line is NOT registered: 26.3's `DebugScreenEntries.register` is
        // private and OML's accessor synthesis covers instance members only (the Fabric branch
        // calls the same private method — see README). The class is kept for when a path opens.
    }

    override fun declareNetwork(network: OMLNetworkRegistry) {
        SyncPayloads.declareReceivers(network)
    }
}
