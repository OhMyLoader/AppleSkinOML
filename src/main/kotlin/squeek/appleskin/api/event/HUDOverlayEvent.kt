package squeek.appleskin.api.event

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.ItemStack
import squeek.appleskin.api.handler.AppleSkinEvent

/**
 * The HUD overlay extension events, fired from the overlay handler right before each overlay draws.
 * Fields and cancel semantics are the upstream ones; only the event bus behind `EVENT` changed.
 */
sealed class HUDOverlayEvent(val x: Int, val y: Int, val context: GuiGraphicsExtractor) {
    var isCanceled = false

    /** If canceled, will stop all rendering of the exhaustion meter. */
    class Exhaustion(
        val exhaustion: Float, x: Int, y: Int, context: GuiGraphicsExtractor,
    ) : HUDOverlayEvent(x, y, context) {
        companion object {
            val EVENT = AppleSkinEvent<Exhaustion>()
        }
    }

    /** If canceled, will stop all rendering of the saturation overlay. */
    class Saturation(
        val saturationLevel: Float, x: Int, y: Int, context: GuiGraphicsExtractor,
    ) : HUDOverlayEvent(x, y, context) {
        companion object {
            val EVENT = AppleSkinEvent<Saturation>()
        }
    }

    /** If canceled, will stop all rendering of the hunger restored overlay. */
    class HungerRestored(
        val currentFoodLevel: Int,
        val itemStack: ItemStack,
        val foodComponent: FoodProperties,
        x: Int, y: Int, context: GuiGraphicsExtractor,
    ) : HUDOverlayEvent(x, y, context) {
        companion object {
            val EVENT = AppleSkinEvent<HungerRestored>()
        }
    }

    /** If canceled, will stop all rendering of the estimated health overlay. */
    class HealthRestored(
        val modifiedHealth: Float,
        val itemStack: ItemStack,
        val foodComponent: FoodProperties,
        x: Int, y: Int, context: GuiGraphicsExtractor,
    ) : HUDOverlayEvent(x, y, context) {
        companion object {
            val EVENT = AppleSkinEvent<HealthRestored>()
        }
    }
}
