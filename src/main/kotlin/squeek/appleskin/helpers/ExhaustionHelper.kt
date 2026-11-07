package squeek.appleskin.helpers

import net.minecraft.world.entity.player.Player
import squeek.appleskin.mixin.FoodDataAccessor

/**
 * Exhaustion is a private field of `FoodData` that vanilla never syncs; the accessor mixin makes it
 * readable and writable. The Fabric branch reached it through a `@Borrowed` mixin that implemented a
 * manipulator interface; on OML the accessor synthesis does the same in two abstract methods.
 */
object ExhaustionHelper {

    fun getExhaustion(player: Player): Float =
        (player.foodData as FoodDataAccessor).getExhaustionLevel()

    fun setExhaustion(player: Player, exhaustion: Float) {
        (player.foodData as FoodDataAccessor).setExhaustionLevel(exhaustion)
    }
}
