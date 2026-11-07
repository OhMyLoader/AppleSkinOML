package squeek.appleskin.mixin

import org.ohmyloader.api.hook.FieldAccess
import org.ohmyloader.api.hook.Hooked

/**
 * Synthesized accessors for `FoodData`'s private `exhaustionLevel` — the one piece of the sync the
 * game's own API does not expose. An interface mixin with `@FieldAccess` methods: the engine
 * synthesizes the bodies inside the target class, so the cast in [ExhaustionHelper] lands on the
 * game's own instances.
 */
@Hooked(target = "net.minecraft.world.food.FoodData")
interface FoodDataAccessor {

    @FieldAccess
    fun getExhaustionLevel(): Float

    @FieldAccess
    fun setExhaustionLevel(value: Float)
}
