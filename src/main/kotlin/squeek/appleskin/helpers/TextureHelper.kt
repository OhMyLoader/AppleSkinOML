package squeek.appleskin.helpers

import net.minecraft.resources.Identifier

object TextureHelper {

    val MOD_ICONS: Identifier = Identifier.fromNamespaceAndPath("appleskin", "textures/icons.png")
    val HUNGER_OUTLINE_SPRITE: Identifier = Identifier.fromNamespaceAndPath("appleskin", "tooltip_hunger_outline")

    // Hunger
    val FOOD_EMPTY_HUNGER_TEXTURE: Identifier = Identifier.withDefaultNamespace("hud/food_empty_hunger")
    val FOOD_HALF_HUNGER_TEXTURE: Identifier = Identifier.withDefaultNamespace("hud/food_half_hunger")
    val FOOD_FULL_HUNGER_TEXTURE: Identifier = Identifier.withDefaultNamespace("hud/food_full_hunger")
    val FOOD_EMPTY_TEXTURE: Identifier = Identifier.withDefaultNamespace("hud/food_empty")
    val FOOD_HALF_TEXTURE: Identifier = Identifier.withDefaultNamespace("hud/food_half")
    val FOOD_FULL_TEXTURE: Identifier = Identifier.withDefaultNamespace("hud/food_full")

    enum class FoodType { EMPTY, HALF, FULL }

    fun getFoodTexture(isRotten: Boolean, type: FoodType): Identifier = when (type) {
        FoodType.EMPTY -> if (isRotten) FOOD_EMPTY_HUNGER_TEXTURE else FOOD_EMPTY_TEXTURE
        FoodType.HALF -> if (isRotten) FOOD_HALF_HUNGER_TEXTURE else FOOD_HALF_TEXTURE
        FoodType.FULL -> if (isRotten) FOOD_FULL_HUNGER_TEXTURE else FOOD_FULL_TEXTURE
    }

    // Hearts
    val HEART_CONTAINER: Identifier = Identifier.withDefaultNamespace("hud/heart/container")
    val HEART_HARDCORE_CONTAINER: Identifier = Identifier.withDefaultNamespace("hud/heart/container_hardcore")
    val HEART_FULL: Identifier = Identifier.withDefaultNamespace("hud/heart/full")
    val HEART_HARDCORE_FULL: Identifier = Identifier.withDefaultNamespace("hud/heart/hardcore_full")
    val HEART_HALF: Identifier = Identifier.withDefaultNamespace("hud/heart/half")
    val HEART_HARDCORE_HALF: Identifier = Identifier.withDefaultNamespace("hud/heart/hardcore_half")

    enum class HeartType { CONTAINER, FULL, HALF }

    fun getHeartTexture(hardcore: Boolean, type: HeartType): Identifier = when (type) {
        HeartType.CONTAINER -> if (hardcore) HEART_HARDCORE_CONTAINER else HEART_CONTAINER
        HeartType.FULL -> if (hardcore) HEART_HARDCORE_FULL else HEART_FULL
        HeartType.HALF -> if (hardcore) HEART_HARDCORE_HALF else HEART_HALF
    }
}
