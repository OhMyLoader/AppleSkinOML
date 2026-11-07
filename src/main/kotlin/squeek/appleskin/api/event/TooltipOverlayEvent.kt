package squeek.appleskin.api.event

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.ItemStack
import squeek.appleskin.api.handler.AppleSkinEvent

/** The tooltip extension events; fields and cancel semantics are the upstream ones. */
sealed class TooltipOverlayEvent(
    val itemStack: ItemStack,
    val defaultFood: FoodProperties,
    val modifiedFood: FoodProperties,
) {
    var isCanceled = false

    /** If canceled, will stop all rendering from happening. */
    class Pre(
        itemStack: ItemStack, defaultFood: FoodProperties, modifiedFood: FoodProperties,
    ) : TooltipOverlayEvent(itemStack, defaultFood, modifiedFood) {
        companion object {
            val EVENT = AppleSkinEvent<Pre>()
        }
    }

    /** If canceled, will reserve space for the food values, but will not render them. */
    class Render(
        itemStack: ItemStack, var x: Int, var y: Int, var context: GuiGraphicsExtractor,
        defaultFood: FoodProperties, modifiedFood: FoodProperties,
    ) : TooltipOverlayEvent(itemStack, defaultFood, modifiedFood) {
        companion object {
            val EVENT = AppleSkinEvent<Render>()
        }
    }
}
