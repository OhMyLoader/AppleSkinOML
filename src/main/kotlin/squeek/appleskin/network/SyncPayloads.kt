package squeek.appleskin.network

import org.ohmyloader.api.network.OMLNetworkRegistry
import org.ohmyloader.api.network.OMLPayloadType
import org.ohmyloader.api.network.codec
import org.ohmyloader.api.tick.OMLScheduler
import org.ohmyloader.api26_3.OMLGame
import squeek.appleskin.helpers.ExhaustionHelper

/** The server's current saturation value for the receiving player. */
data class SaturationSyncPayload(val saturation: Float)

/** The server's current exhaustion value for the receiving player (vanilla never syncs it). */
data class ExhaustionSyncPayload(val exhaustion: Float)

/** Whether the `naturalHealthRegeneration` game rule is on (the client cannot read server rules). */
data class NaturalRegenerationSyncPayload(val naturalRegeneration: Boolean)

/**
 * The three clientbound payloads of the sync layer. The Fabric branch declared each as a record
 * implementing `CustomPacketPayload` with a `StreamCodec`; on OML the payload is a data class and
 * the channel is an [OMLPayloadType] — the codec shuffles the same floats.
 */
object SyncPayloads {

    val SATURATION = OMLPayloadType(
        "saturation",
        codec(
            { buffer, value -> buffer.writeFloat(value.saturation) },
            { buffer -> SaturationSyncPayload(buffer.readFloat()) }),
    )

    val EXHAUSTION = OMLPayloadType(
        "exhaustion",
        codec(
            { buffer, value -> buffer.writeFloat(value.exhaustion) },
            { buffer -> ExhaustionSyncPayload(buffer.readFloat()) }),
    )

    val NATURAL_REGENERATION = OMLPayloadType(
        "natural_regeneration",
        codec(
            { buffer, value -> buffer.writeBoolean(value.naturalRegeneration) },
            { buffer -> NaturalRegenerationSyncPayload(buffer.readBoolean()) }),
    )

    /** The client's half of the sync: applying the server's authoritative values to the local player. */
    fun declareReceivers(network: OMLNetworkRegistry) {
        network.serverToClient(EXHAUSTION) { value, _ ->
            // The game objects move to the client thread, exactly what the Fabric branch's
            // `context.client().execute(...)` did.
            OMLScheduler.client.schedule {
                OMLGame.client?.player?.let { ExhaustionHelper.setExhaustion(it, value.exhaustion) }
            }
        }
        network.serverToClient(SATURATION) { value, _ ->
            OMLScheduler.client.schedule {
                OMLGame.client?.player?.foodData?.setSaturation(value.saturation)
            }
        }
        network.serverToClient(NATURAL_REGENERATION) { value, _ ->
            ClientSyncHandler.naturalRegeneration = value.naturalRegeneration
        }
    }
}
