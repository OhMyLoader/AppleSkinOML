package squeek.appleskin.api.event

import net.minecraft.world.entity.player.Player
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.ItemStack
import squeek.appleskin.api.handler.AppleSkinEvent

/**
 * Can be used to customize the displayed hunger/saturation values of foods.
 * Called whenever the food values of items are being determined.
 */
class FoodValuesEvent(
    val player: Player,
    val itemStack: ItemStack,
    defaultFoodValues: FoodProperties,
    var modifiedFoodComponent: FoodProperties,
) {
    var defaultFoodComponent: FoodProperties = defaultFoodValues

    companion object {
        val EVENT = AppleSkinEvent<FoodValuesEvent>()
    }
}
