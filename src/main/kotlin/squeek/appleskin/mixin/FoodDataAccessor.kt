package squeek.appleskin.mixin

import org.ohmyloader.api.mixin.Accessor
import org.ohmyloader.api.mixin.Mixin

/**
 * Synthesized accessors for `FoodData`'s private `exhaustionLevel` — the one piece of the sync the
 * game's own API does not expose. An interface mixin with `@Accessor` methods: the engine
 * synthesizes the bodies inside the target class, so the cast in [ExhaustionHelper] lands on the
 * game's own instances.
 */
@Mixin(target = "net.minecraft.world.food.FoodData")
interface FoodDataAccessor {

    @Accessor
    fun getExhaustionLevel(): Float

    @Accessor
    fun setExhaustionLevel(value: Float)
}
