package squeek.appleskin.helpers

import net.minecraft.core.component.DataComponents
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.food.FoodData
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.Consumable
import net.minecraft.world.item.component.Consumables
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect
import squeek.appleskin.api.event.FoodValuesEvent
import squeek.appleskin.network.ClientSyncHandler
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.nextUp

object FoodHelper {

    fun isFood(itemStack: ItemStack): Boolean =
        itemStack.has(DataComponents.FOOD) && itemStack.has(DataComponents.CONSUMABLE)

    fun canConsume(player: Player, foodComponent: FoodProperties): Boolean =
        player.canEat(foodComponent.canAlwaysEat())

    private val EMPTY_FOOD_COMPONENT = FoodProperties.Builder().build()
    private val DEFAULT_CONSUMABLE_COMPONENT = Consumables.DEFAULT_FOOD

    /**
     * Assumes itemStack is known to be a food, always returns a non-null ConsumableFood
     */
    fun getDefaultFoodValues(itemStack: ItemStack): ConsumableFood =
        ConsumableFood(
            itemStack.getOrDefault(DataComponents.FOOD, EMPTY_FOOD_COMPONENT),
            itemStack.getOrDefault(DataComponents.CONSUMABLE, DEFAULT_CONSUMABLE_COMPONENT),
        )

    class QueriedFoodResult(
        val defaultFoodComponent: FoodProperties,
        val modifiedFoodComponent: FoodProperties,
        val consumableComponent: Consumable,
        val itemStack: ItemStack,
    )

    fun query(itemStack: ItemStack, player: Player): QueriedFoodResult? {
        if (!isFood(itemStack)) return null

        val defaultFood = getDefaultFoodValues(itemStack)

        val foodValuesEvent = FoodValuesEvent(player, itemStack, defaultFood.food, defaultFood.food)
        FoodValuesEvent.EVENT.invoker().interact(foodValuesEvent)

        return QueriedFoodResult(
            foodValuesEvent.defaultFoodComponent, foodValuesEvent.modifiedFoodComponent,
            defaultFood.consumable, itemStack,
        )
    }

    fun isRotten(consumableComponent: Consumable): Boolean {
        for (effect in consumableComponent.onConsumeEffects()) {
            if (effect !is ApplyStatusEffectsConsumeEffect) continue

            for (statusEffect in effect.effects) {
                if (statusEffect.effect.value().category == MobEffectCategory.HARMFUL) return true
            }
        }
        return false
    }

    fun getEstimatedHealthIncrement(player: Player, consumableFood: ConsumableFood): Float {
        // 26.4 dropped `isHurt()`; the question is the same one — food only adds health when the
        // player is below full health.
        if (player.health >= player.maxHealth) return 0f

        val stats: FoodData = player.foodData

        val foodLevel = minOf(stats.foodLevel + consumableFood.food.nutrition(), 20)
        var healthIncrement = 0f

        // health for natural regen
        if (foodLevel >= 18.0f && ClientSyncHandler.naturalRegeneration) {
            val saturationLevel = minOf(
                stats.saturationLevel + consumableFood.food.saturation(), foodLevel.toFloat(),
            )
            val exhaustionLevel = ExhaustionHelper.getExhaustion(player)
            healthIncrement = getEstimatedHealthIncrement(foodLevel, saturationLevel, exhaustionLevel)
        }

        // health for regeneration effect
        for (effect in consumableFood.consumable.onConsumeEffects()) {
            if (effect !is ApplyStatusEffectsConsumeEffect) continue

            for (statusEffect in effect.effects) {
                if (statusEffect.effect == MobEffects.REGENERATION) {
                    val amplifier = statusEffect.amplifier
                    val duration = statusEffect.duration

                    // Refer: https://minecraft.fandom.com/wiki/Regeneration
                    // Refer: net.minecraft.entity.effect.StatusEffect.canApplyUpdateEffect
                    healthIncrement += floor((duration / maxOf(50 shr amplifier, 1)).toDouble()).toFloat()
                    break
                }
            }
        }

        return healthIncrement
    }

    const val REGEN_EXHAUSTION_INCREMENT = 6.0f
    const val MAX_EXHAUSTION = 4.0f

    fun getEstimatedHealthIncrement(foodLevel: Int, saturationLevel: Float, exhaustionLevel: Float): Float {
        var health = 0f
        var level = foodLevel
        var saturation = saturationLevel
        var exhaustion = exhaustionLevel

        if (!exhaustion.isFinite() || !saturation.isFinite()) return 0f

        while (level >= 18) {
            while (exhaustion > MAX_EXHAUSTION) {
                exhaustion -= MAX_EXHAUSTION
                if (saturation > 0) saturation = maxOf(saturation - 1, 0f) else level -= 1
            }
            // Without this Float.compare, it's possible for this function to get stuck in an infinite loop
            // if saturationLevel is small enough that exhaustionLevel does not actually change representation
            // when it's incremented. This Float.compare makes it so we treat such close-to-zero values as zero.
            if (level >= 20 && saturation.compareTo(java.lang.Float.MIN_NORMAL) > 0) {
                // fast regen health
                //
                // Because only health and exhaustion increase in this branch, we know that we will
                // enter this branch again and again on each iteration if exhaustion is not
                // incremented above MAX_EXHAUSTION before the next iteration. So, instead of
                // actually performing those iterations, we can calculate the number of iterations
                // it would take to reach max exhaustion, and add all the health/exhaustion in one
                // go (see the upstream comment for the full rationale and the floating-point
                // caveats).
                val limitedSaturation = minOf(saturation, REGEN_EXHAUSTION_INCREMENT)
                val exhaustionUntilAboveMax = MAX_EXHAUSTION.nextUp() - exhaustion
                val numIterationsUntilAboveMax =
                    maxOf(1, ceil((exhaustionUntilAboveMax / limitedSaturation).toDouble()).toInt())

                health += (limitedSaturation / REGEN_EXHAUSTION_INCREMENT) * numIterationsUntilAboveMax
                exhaustion += limitedSaturation * numIterationsUntilAboveMax
            } else if (level >= 18) {
                // slow regen health
                health += 1f
                exhaustion += REGEN_EXHAUSTION_INCREMENT
            }
        }
        return health
    }
}
