package squeek.appleskin.network

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.gamerules.GameRules
import org.ohmyloader.api.core.network.OMLNetwork
import org.ohmyloader.api.event.Events
import squeek.appleskin.helpers.ExhaustionHelper
import java.util.*
import kotlin.math.abs

/**
 * The server half of the sync. The Fabric branch hooked `ServerPlayer.tick` and
 * `PlayerList.placeNewPlayer` with mixins; on OML the same two sites are the `LEVEL_TICK` (the
 * server's loaded dimensions each tick, the players iterated here) and `PLAYER_JOINED` (the tail
 * of `placeNewPlayer`) events, so no mixin is needed for this layer.
 */
object SyncHandler {

    private val lastSaturationLevels = HashMap<UUID, Float>()
    private val lastExhaustionLevels = HashMap<UUID, Float>()
    private var naturalRegeneration = true

    fun init() {
        Events.LEVEL_TICK.register { event ->
            val level = event.level
            if (!level.isClientSide) onServerWorldTick(level as ServerLevel)
        }
        Events.PLAYER_JOINED.register { event -> onPlayerLoggedIn(event.player) }
    }

    /**
     * Sync saturation (vanilla MC only syncs when it hits 0).
     * Sync exhaustion (vanilla MC does not sync it at all).
     */
    fun onPlayerUpdate(player: ServerPlayer) {
        val lastSaturationLevel = lastSaturationLevels[player.uuid]
        val lastExhaustionLevel = lastExhaustionLevels[player.uuid]

        val saturation = player.foodData.saturationLevel
        if (lastSaturationLevel == null || lastSaturationLevel != saturation) {
            OMLNetwork.sendToPlayer(SyncPayloads.SATURATION, SaturationSyncPayload(saturation), player.scoreboardName)
            lastSaturationLevels[player.uuid] = saturation
        }

        val exhaustionLevel = ExhaustionHelper.getExhaustion(player)
        if (lastExhaustionLevel == null || abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f) {
            OMLNetwork.sendToPlayer(
                SyncPayloads.EXHAUSTION,
                ExhaustionSyncPayload(exhaustionLevel),
                player.scoreboardName
            )
            lastExhaustionLevels[player.uuid] = exhaustionLevel
        }
    }

    fun onPlayerLoggedIn(player: ServerPlayer) {
        lastSaturationLevels.remove(player.uuid)
        lastExhaustionLevels.remove(player.uuid)
        // Assumed to be true by default, so we only need to update the client if it's actually false
        if (!naturalRegeneration) {
            OMLNetwork.sendToPlayer(
                SyncPayloads.NATURAL_REGENERATION, NaturalRegenerationSyncPayload(false), player.scoreboardName,
            )
        }
    }

    fun onServerWorldTick(world: ServerLevel) {
        val cur = world.gameRules.get(GameRules.NATURAL_HEALTH_REGENERATION)
        if (naturalRegeneration != cur) {
            world.players().forEach { player ->
                OMLNetwork.sendToPlayer(
                    SyncPayloads.NATURAL_REGENERATION,
                    NaturalRegenerationSyncPayload(cur),
                    player.scoreboardName
                )
            }
            naturalRegeneration = cur
        }
        world.players().forEach(::onPlayerUpdate)
    }
}
