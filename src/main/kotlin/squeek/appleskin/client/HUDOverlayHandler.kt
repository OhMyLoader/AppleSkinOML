package squeek.appleskin.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.GuiPipeline
import net.minecraft.world.Difficulty
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.food.FoodData
import squeek.appleskin.ModConfig
import squeek.appleskin.api.event.HUDOverlayEvent
import squeek.appleskin.helpers.*
import squeek.appleskin.helpers.TextureHelper.FoodType
import squeek.appleskin.helpers.TextureHelper.HeartType
import squeek.appleskin.util.IntPoint
import java.util.*
import kotlin.math.ceil

/**
 * The HUD overlays, ported line-for-line from the Fabric branch: the only structural change is that
 * the instance is an object fed by [squeek.appleskin.mixin.HudMixin] instead of Fabric's mixin
 * calling `INSTANCE`.
 */
object HUDOverlayHandler {

    private var unclampedFlashAlpha = 0f
    private var flashAlpha = 0f
    private var alphaDir = 1

    val barOffsets = OffsetsCache()
    val heldFood = HeldFoodCache()

    fun init() {
    }

    fun onPreRenderFood(context: GuiGraphicsExtractor, player: Player, top: Int, right: Int) {
        if (!ModConfig.showFoodExhaustionHudUnderlay) return

        val exhaustion = ExhaustionHelper.getExhaustion(player)

        // Notify everyone that we should render exhaustion hud overlay
        val renderEvent = HUDOverlayEvent.Exhaustion(exhaustion, right, top, context)
        HUDOverlayEvent.Exhaustion.EVENT.invoker().interact(renderEvent)
        if (!renderEvent.isCanceled) {
            drawExhaustionOverlay(context, renderEvent.exhaustion, renderEvent.x, renderEvent.y, 1f)
        }
    }

    fun onRenderFood(context: GuiGraphicsExtractor, player: Player, top: Int, right: Int) {
        if (!shouldRenderAnyOverlays()) return

        val mc = Minecraft.getInstance()
        val stats: FoodData = player.foodData

        // notify everyone that we should render saturation hud overlay
        val saturationRenderEvent = HUDOverlayEvent.Saturation(stats.saturationLevel, right, top, context)

        // cancel render overlay event when configuration disabled.
        if (!ModConfig.showSaturationHudOverlay) saturationRenderEvent.isCanceled = true

        // notify everyone that we should render saturation hud overlay
        if (!saturationRenderEvent.isCanceled) {
            HUDOverlayEvent.Saturation.EVENT.invoker().interact(saturationRenderEvent)
        }

        // draw saturation overlay
        if (!saturationRenderEvent.isCanceled) {
            drawSaturationOverlay(
                context, 0f, saturationRenderEvent.saturationLevel, mc,
                saturationRenderEvent.x, saturationRenderEvent.y, 1f, mc.gui.hud.guiTicks,
            )
        }

        // try to get the item stack in the player hand
        val result = heldFood.result(mc.gui.hud.guiTicks, player) ?: run {
            resetFlash()
            return
        }

        // restored hunger/saturation overlay while holding food
        if (ModConfig.showFoodValuesHudOverlay) {
            // notify everyone that we should render hunger hud overlay
            val hungerRenderEvent = HUDOverlayEvent.HungerRestored(
                stats.foodLevel, result.itemStack, result.modifiedFoodComponent, right, top, context,
            )
            HUDOverlayEvent.HungerRestored.EVENT.invoker().interact(hungerRenderEvent)
            if (hungerRenderEvent.isCanceled) return

            // calculate the final hunger and saturation
            val foodHunger = hungerRenderEvent.foodComponent.nutrition()
            val foodSaturationIncrement = hungerRenderEvent.foodComponent.saturation()

            // draw hunger overlay
            drawHungerOverlay(
                context, foodHunger, hungerRenderEvent.currentFoodLevel, mc,
                hungerRenderEvent.x, hungerRenderEvent.y, flashAlpha,
                FoodHelper.isRotten(result.consumableComponent), mc.gui.hud.guiTicks,
            )

            val newFoodValue = stats.foodLevel + foodHunger
            val newSaturationValue = stats.saturationLevel + foodSaturationIncrement

            // draw saturation overlay of gained
            if (!saturationRenderEvent.isCanceled) {
                val saturationGained =
                    if (newSaturationValue > newFoodValue) newFoodValue - stats.saturationLevel
                    else foodSaturationIncrement
                drawSaturationOverlay(
                    context, saturationGained, saturationRenderEvent.saturationLevel, mc,
                    saturationRenderEvent.x, saturationRenderEvent.y, flashAlpha, mc.gui.hud.guiTicks,
                )
            }
        }
    }

    fun onRenderHealth(
        context: GuiGraphicsExtractor, player: Player, left: Int, top: Int, lines: Int,
        regeneratingHeartIndex: Int, maxHealth: Float, lastHealth: Int, health: Int,
        absorption: Int, blinking: Boolean,
    ) {
        if (!shouldRenderAnyOverlays()) return

        val mc = Minecraft.getInstance()

        // try to get the item stack in the player hand
        val result = heldFood.result(mc.gui.hud.guiTicks, player) ?: run {
            resetFlash()
            return
        }

        // draw health overlay if needed
        if (shouldShowEstimatedHealth(player, mc.gui.hud.guiTicks)) {
            val foodHealthIncrement =
                FoodHelper.getEstimatedHealthIncrement(
                    player,
                    ConsumableFood(result.modifiedFoodComponent, result.consumableComponent)
                )
            val currentHealth = player.health
            val modifiedHealth = minOf(currentHealth + foodHealthIncrement, player.maxHealth)

            // only create object when the estimated health is successfully
            var healthRenderEvent: HUDOverlayEvent.HealthRestored? = null
            if (currentHealth < modifiedHealth) {
                healthRenderEvent = HUDOverlayEvent.HealthRestored(
                    modifiedHealth, result.itemStack, result.modifiedFoodComponent, left, top, context,
                )
            }

            // notify everyone that we should render estimated health hud
            healthRenderEvent?.let { HUDOverlayEvent.HealthRestored.EVENT.invoker().interact(it) }

            if (healthRenderEvent != null && !healthRenderEvent.isCanceled) {
                drawHealthOverlay(
                    context, player.health, healthRenderEvent.modifiedHealth, mc,
                    healthRenderEvent.x, healthRenderEvent.y, flashAlpha, mc.gui.hud.guiTicks,
                )
            }
        }
    }

    fun drawSaturationOverlay(
        context: GuiGraphicsExtractor, saturationGained: Float, saturationLevel: Float,
        mc: Minecraft, right: Int, top: Int, alpha: Float, guiTicks: Int,
    ) {
        if (saturationLevel + saturationGained < 0) return

        val alphaColor = ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, alpha)

        val modifiedSaturation = maxOf(0f, minOf(saturationLevel + saturationGained, 20f))

        var startSaturationBar = 0
        val endSaturationBar = ceil((modifiedSaturation / 2.0f).toDouble()).toInt()

        // when require rendering the gained saturation, start should relocation to current saturation tail.
        if (saturationGained != 0f) startSaturationBar = maxOf((saturationLevel / 2.0f).toInt(), 0)

        val iconSize = 9

        val foodBarOffsets = barOffsets.foodBarOffsets(guiTicks, mc.player ?: return)
        for (i in startSaturationBar until endSaturationBar) {
            // gets the offset that needs to be render of icon
            val offset: IntPoint = if (i < foodBarOffsets.size) foodBarOffsets[i] else IntPoint()

            val x = right + offset.x
            val y = top + offset.y

            var u = 0
            val v = 0

            val effectiveSaturationOfBar = (modifiedSaturation / 2.0f) - i

            if (effectiveSaturationOfBar >= 1) u = 3 * iconSize
            else if (effectiveSaturationOfBar > .5) u = 2 * iconSize
            else if (effectiveSaturationOfBar > .25) u = 1 * iconSize

            context.blit(
                GuiPipeline.TEXTURED, TextureHelper.MOD_ICONS, x, y, u.toFloat(), v.toFloat(),
                iconSize, iconSize, 256, 256, alphaColor,
            )
        }
    }

    fun drawHungerOverlay(
        context: GuiGraphicsExtractor, hungerRestored: Int, foodLevel: Int, mc: Minecraft,
        right: Int, top: Int, alpha: Float, useRottenTextures: Boolean, guiTicks: Int,
    ) {
        if (hungerRestored <= 0) return

        val alphaColor = ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, alpha)

        val modifiedFood = maxOf(0, minOf(20, foodLevel + hungerRestored))

        val startFoodBars = maxOf(0, foodLevel / 2)
        val endFoodBars = ceil((modifiedFood / 2.0f).toDouble()).toInt()

        val iconSize = 9

        val foodBarOffsets = barOffsets.foodBarOffsets(guiTicks, mc.player ?: return)
        for (i in startFoodBars until endFoodBars) {
            // gets the offset that needs to be render of icon
            val offset: IntPoint = if (i < foodBarOffsets.size) foodBarOffsets[i] else IntPoint()

            val x = right + offset.x
            val y = top + offset.y

            val backgroundSprite = TextureHelper.getFoodTexture(useRottenTextures, FoodType.EMPTY)

            // very faint background
            val bgColor = ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, alpha * 0.25f)
            context.blitSprite(GuiPipeline.TEXTURED, backgroundSprite, x, y, iconSize, iconSize, bgColor)

            val isHalf = i * 2 + 1 == modifiedFood
            val iconSprite =
                TextureHelper.getFoodTexture(useRottenTextures, if (isHalf) FoodType.HALF else FoodType.FULL)

            context.blitSprite(GuiPipeline.TEXTURED, iconSprite, x, y, iconSize, iconSize, alphaColor)
        }
    }

    fun drawHealthOverlay(
        context: GuiGraphicsExtractor, health: Float, modifiedHealth: Float, mc: Minecraft,
        right: Int, top: Int, alpha: Float, guiTicks: Int,
    ) {
        if (modifiedHealth <= health) return

        val alphaColor = ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, alpha)

        val fixedModifiedHealth = ceil(modifiedHealth.toDouble()).toInt()
        val self = mc.player ?: return
        val isHardcore = self.level().levelData.isHardcore

        val startHealthBars = maxOf(0, ceil((health / 2.0f).toDouble()).toInt())
        val endHealthBars = maxOf(0, ceil((modifiedHealth / 2.0f).toDouble()).toInt())

        val iconSize = 9

        val healthBarOffsets = barOffsets.healthBarOffsets(guiTicks, mc.player ?: return)
        for (i in startHealthBars until endHealthBars) {
            // gets the offset that needs to be render of icon
            val offset: IntPoint = if (i < healthBarOffsets.size) healthBarOffsets[i] else IntPoint()

            val x = right + offset.x
            val y = top + offset.y

            val backgroundSprite = TextureHelper.getHeartTexture(isHardcore, HeartType.CONTAINER)

            // very faint background
            val bgColor = ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, alpha * 0.25f)
            context.blitSprite(GuiPipeline.TEXTURED, backgroundSprite, x, y, iconSize, iconSize, bgColor)

            val isHalf = i * 2 + 1 == fixedModifiedHealth
            val iconSprite = TextureHelper.getHeartTexture(isHardcore, if (isHalf) HeartType.HALF else HeartType.FULL)

            context.blitSprite(GuiPipeline.TEXTURED, iconSprite, x, y, iconSize, iconSize, alphaColor)
        }
    }

    fun drawExhaustionOverlay(context: GuiGraphicsExtractor, exhaustion: Float, right: Int, top: Int, alpha: Float) {
        val maxExhaustion = FoodHelper.MAX_EXHAUSTION
        // clamp between 0 and 1
        val ratio = minOf(1f, maxOf(0f, exhaustion / maxExhaustion))
        val width = (ratio * 81).toInt()
        val height = 9

        val color = ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, 0.75f)
        context.blit(
            GuiPipeline.TEXTURED, TextureHelper.MOD_ICONS, right - width, top,
            (81 - width).toFloat(), 18f, width, height, 256, 256, color,
        )
    }

    private fun shouldRenderAnyOverlays(): Boolean =
        ModConfig.showFoodValuesHudOverlay || ModConfig.showSaturationHudOverlay || ModConfig.showFoodHealthHudOverlay

    fun onClientTick() {
        unclampedFlashAlpha += alphaDir * 0.125f
        if (unclampedFlashAlpha >= 1.5f) {
            alphaDir = -1
        } else if (unclampedFlashAlpha <= -0.5f) {
            alphaDir = 1
        }
        flashAlpha = maxOf(0f, minOf(1f, unclampedFlashAlpha)) *
                maxOf(0f, minOf(1f, ModConfig.maxHudOverlayFlashAlpha))
    }

    fun resetFlash() {
        unclampedFlashAlpha = 0f
        flashAlpha = 0f
        alphaDir = 1
    }

    private fun shouldShowEstimatedHealth(player: Player, guiTicks: Int): Boolean {
        // then configuration cancel the render event
        if (!ModConfig.showFoodHealthHudOverlay) return false

        // Offsets size is set to zero intentionally to disable rendering when health is infinite.
        if (barOffsets.healthBarOffsets(guiTicks, player).isEmpty()) return false

        val stats: FoodData = player.foodData

        // in the `PEACEFUL` mode, health will restore faster
        if (player.level().difficulty == Difficulty.PEACEFUL) return false

        // when player has any changes health amount by any case can't show estimated health
        // because player will confused how much of restored/damaged healths
        if (stats.foodLevel >= 18) return false

        if (player.hasEffect(MobEffects.POISON)) return false

        if (player.hasEffect(MobEffects.WITHER)) return false

        if (player.hasEffect(MobEffects.REGENERATION)) return false

        return true
    }

    open class OffsetsCache {
        protected val foodBarOffsets = Vector<IntPoint>()
        protected val healthBarOffsets = Vector<IntPoint>()
        var lastGuiTick = 0
        protected val random = Random()

        protected fun generate(guiTicks: Int, player: Player) {
            // Note: Both health and food offsets are generated together
            // because the PRNG used for the health icon offsets affects
            // the PRNG of the food icon offsets. By generating the offsets
            // together, we can match the PRNG of the Vanilla HUD.

            val preferHealthBars = 10
            val preferFoodBars = 10

            val maxHealth = player.maxHealth
            val absorptionHealth = ceil(player.absorptionAmount.toDouble()).toFloat()

            var healthBars = ceil(((maxHealth + absorptionHealth) / 2.0f).toDouble()).toInt()
            // When maxHealth + absorptionHealth is greater than Integer.INT_MAX,
            // Minecraft will disable heart rendering due to a quirk of MathHelper.ceil.
            // We have a much lower threshold since there's no reason to get the offsets
            // for thousands of hearts.
            // Note: Infinite and > INT_MAX absorption has been seen in the wild.
            // This will effectively disable rendering whenever health is unexpectedly large.
            if (healthBars !in 0..1000) {
                healthBars = 0
            }

            val healthRows = ceil(healthBars.toDouble() / preferHealthBars.toDouble()).toInt()

            val healthRowHeight = maxOf(10 - (healthRows - 2), 3)

            var shouldAnimatedHealth = false
            var shouldAnimatedFood = false

            // when some mods using custom render, we need to least provide an option to cancel animation
            if (ModConfig.showVanillaAnimationsOverlay) {
                val hungerManager: FoodData = player.foodData

                // in vanilla saturation level is zero will show hunger animation
                val saturationLevel = hungerManager.saturationLevel
                val foodLevel = hungerManager.foodLevel
                shouldAnimatedFood = saturationLevel <= 0.0f && guiTicks % (foodLevel * 3 + 1) == 0

                // in vanilla health is too low (below 5) will show heartbeat animation
                // when regeneration will also show heartbeat animation, but we don't need now
                shouldAnimatedHealth = ceil(player.health.toDouble()) <= 4
            }

            // hard code in `InGameHUD`
            random.setSeed(guiTicks * 312871L)

            // adjust the size
            if (healthBarOffsets.size != healthBars) healthBarOffsets.setSize(healthBars)

            if (foodBarOffsets.size != preferFoodBars) foodBarOffsets.setSize(preferFoodBars)

            // left alignment, multiple rows, reverse
            for (i in healthBars - 1 downTo 0) {
                val row = ceil((i + 1).toDouble() / preferHealthBars.toDouble()).toInt() - 1
                val x = i % preferHealthBars * 8
                var y = -(row * healthRowHeight)
                // apply the animated offset
                if (shouldAnimatedHealth) y += random.nextInt(2)

                // reuse the point object to reduce memory usage
                var point = healthBarOffsets[i]
                if (point == null) {
                    point = IntPoint()
                    healthBarOffsets[i] = point
                }

                point.x = x
                point.y = y
            }

            // right alignment, single row
            for (i in 0 until preferFoodBars) {
                val x = -(i * 8) - 9
                var y = 0

                // apply the animated offset
                if (shouldAnimatedFood) y += random.nextInt(3) - 1

                // reuse the point object to reduce memory usage
                var point = foodBarOffsets[i]
                if (point == null) {
                    point = IntPoint()
                    foodBarOffsets[i] = point
                }

                point.x = x
                point.y = y
            }
        }

        fun healthBarOffsets(guiTick: Int, player: Player): Vector<IntPoint> {
            if (guiTick != lastGuiTick) {
                generate(guiTick, player)
                lastGuiTick = guiTick
            }
            return this.healthBarOffsets
        }

        fun foodBarOffsets(guiTicks: Int, player: Player): Vector<IntPoint> {
            if (guiTicks != lastGuiTick) {
                generate(guiTicks, player)
                lastGuiTick = guiTicks
            }
            return this.foodBarOffsets
        }
    }

    open class HeldFoodCache {
        var result: FoodHelper.QueriedFoodResult? = null
            protected set
        var lastGuiTick = 0
            protected set

        protected fun query(player: Player) {
            // try to get the item stack in the player hand
            var heldItem = player.mainHandItem
            var heldFood = FoodHelper.query(heldItem, player)
            var canConsume = heldFood != null && FoodHelper.canConsume(player, heldFood.modifiedFoodComponent)
            if (ModConfig.showFoodValuesHudOverlayWhenOffhand && !canConsume) {
                heldItem = player.offhandItem
                heldFood = FoodHelper.query(heldItem, player)
                canConsume = heldFood != null && FoodHelper.canConsume(player, heldFood.modifiedFoodComponent)
            }

            val shouldRenderHeldItemValues = !heldItem.isEmpty && canConsume
            if (!shouldRenderHeldItemValues) {
                this.result = null
                return
            }

            this.result = heldFood
        }

        fun result(guiTick: Int, player: Player): FoodHelper.QueriedFoodResult? {
            if (guiTick != lastGuiTick) {
                query(player)
                lastGuiTick = guiTick
            }
            return this.result
        }
    }
}
