package squeek.appleskin

import org.ohmyloader.api.core.config.OMLConfig

/**
 * The mod config, declared through the loader's TOML config instead of jankson + cloth-config
 * screens: the entries and their comments are the upstream ones, one declare + one read each.
 */
object ModConfig {
    var showFoodValuesInTooltip = true; private set
    var showFoodValuesInTooltipAlways = true; private set
    var showSaturationHudOverlay = true; private set
    var showFoodValuesHudOverlay = true; private set
    var showFoodValuesHudOverlayWhenOffhand = true; private set
    var showFoodExhaustionHudUnderlay = true; private set
    var showFoodHealthHudOverlay = true; private set
    var showVanillaAnimationsOverlay = true; private set
    var maxHudOverlayFlashAlpha = 0.65f; private set

    fun init(config: OMLConfig) {
        config.define(
            "showFoodValuesInTooltip",
            true,
            "If true, shows the hunger and saturation values of food in its tooltip while holding SHIFT"
        )
        config.define(
            "showFoodValuesInTooltipAlways",
            true,
            "If true, shows the hunger and saturation values of food in its tooltip automatically (without needing to hold SHIFT)"
        )
        config.define(
            "showSaturationHudOverlay",
            true,
            "If true, shows your current saturation level overlayed on the hunger bar"
        )
        config.define(
            "showFoodValuesHudOverlay",
            true,
            "If true, shows the hunger (and saturation if showSaturationHudOverlay is true) that would be restored by food you are currently holding"
        )
        config.define(
            "showFoodValuesHudOverlayWhenOffhand",
            true,
            "If true, enables the hunger/saturation/health overlays for food in your off-hand"
        )
        config.define(
            "showFoodExhaustionHudUnderlay",
            true,
            "If true, shows your food exhaustion as a progress bar behind the hunger bar"
        )
        config.define(
            "showFoodHealthHudOverlay",
            true,
            "If true, shows estimated health restored by food on the health bar"
        )
        config.define(
            "showVanillaAnimationsOverlay",
            true,
            "If true, health/hunger overlay will shake to match Minecraft's icon animations"
        )
        config.define(
            "maxHudOverlayFlashAlpha",
            0.65,
            "Alpha value of the flashing icons at their most visible point (1.0 = fully opaque, 0.0 = fully transparent)"
        )

        showFoodValuesInTooltip = config.getBoolean("showFoodValuesInTooltip")
        showFoodValuesInTooltipAlways = config.getBoolean("showFoodValuesInTooltipAlways")
        showSaturationHudOverlay = config.getBoolean("showSaturationHudOverlay")
        showFoodValuesHudOverlay = config.getBoolean("showFoodValuesHudOverlay")
        showFoodValuesHudOverlayWhenOffhand = config.getBoolean("showFoodValuesHudOverlayWhenOffhand")
        showFoodExhaustionHudUnderlay = config.getBoolean("showFoodExhaustionHudUnderlay")
        showFoodHealthHudOverlay = config.getBoolean("showFoodHealthHudOverlay")
        showVanillaAnimationsOverlay = config.getBoolean("showVanillaAnimationsOverlay")
        maxHudOverlayFlashAlpha = config.getDouble("maxHudOverlayFlashAlpha").toFloat()
    }
}
