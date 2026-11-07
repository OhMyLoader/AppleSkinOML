package squeek.appleskin.client

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentContents
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.FormattedCharSink
import net.minecraft.util.StringDecomposer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.Consumable
import net.minecraft.world.item.component.TooltipDisplay
import org.joml.Matrix3x2fStack
import squeek.appleskin.ModConfig
import squeek.appleskin.api.event.TooltipOverlayEvent
import squeek.appleskin.helpers.ColorHelper
import squeek.appleskin.helpers.FoodHelper
import squeek.appleskin.helpers.KeyHelper
import squeek.appleskin.helpers.TextureHelper
import squeek.appleskin.helpers.TextureHelper.FoodType
import java.util.ArrayList

object TooltipOverlayHandler {

    abstract class EmptyText : Component {
        override fun getStyle(): Style = Style.EMPTY

        override fun getContents(): ComponentContents = PlainTextContents.EMPTY

        override fun getSiblings(): List<Component> = emptySiblings

        companion object {
            private val emptySiblings: List<Component> = ArrayList()
        }
    }

    // Bind to text line, because food overlay must apply line offset of all case.
    class FoodOverlayTextComponent(val foodOverlay: FoodOverlay) : EmptyText(), FormattedCharSequence {

        override fun getVisualOrderText(): FormattedCharSequence = this

        override fun accept(visitor: FormattedCharSink): Boolean =
            StringDecomposer.iterateFormatted(this, style, visitor)
    }

    class FoodOverlay(
        val itemStack: ItemStack,
        val defaultFood: FoodProperties,
        val modifiedFood: FoodProperties,
        val consumableComponent: Consumable,
        player: Player,
    ) : ClientTooltipComponent, TooltipComponent {

        private val biggestHunger = maxOf(defaultFood.nutrition(), modifiedFood.nutrition())
        private val biggestSaturationIncrement = maxOf(defaultFood.saturation(), modifiedFood.saturation())

        var hungerBars = Math.ceil((Math.abs(biggestHunger) / 2f).toDouble()).toInt()
            private set
        var hungerBarsText: String? = null
            private set
        var saturationBars = Math.ceil((Math.abs(biggestSaturationIncrement) / 2f).toDouble()).toInt()
            private set
        var saturationBarsText: String? = null
            private set

        init {
            if (hungerBars > 10) {
                hungerBarsText = "x" + ((if (biggestHunger < 0) -1 else 1) * hungerBars)
                hungerBars = 1
            }

            if (saturationBars > 10 || saturationBars == 0) {
                saturationBarsText = "x" + ((if (biggestSaturationIncrement < 0) -1 else 1) * saturationBars)
                saturationBars = 1
            }
        }

        fun shouldRenderHungerBars(): Boolean = hungerBars > 0

        override fun getHeight(textRenderer: Font): Int =
            // hunger + spacing + saturation + arbitrary spacing, for some reason 3 extra looks best
            9 + 1 + 7 + 3

        override fun getWidth(textRenderer: Font): Int {
            var hungerBarLength = hungerBars * 9
            hungerBarsText?.let { hungerBarLength += textRenderer.width(it) }
            var saturationBarLength = saturationBars * 7
            saturationBarsText?.let { saturationBarLength += textRenderer.width(it) }
            return maxOf(hungerBarLength, saturationBarLength)
        }

        override fun extractImage(
            textRenderer: Font, x: Int, y: Int, width: Int, height: Int, context: GuiGraphicsExtractor,
        ) {
            onRenderTooltip(context, this, x, y, textRenderer)
        }
    }

    @JvmStatic
    fun onItemTooltip(
        hoveredStack: ItemStack, player: Player, type: TooltipFlag,
        tooltip: MutableList<Component>,
    ) {
        if (!shouldShowTooltip(hoveredStack, type)) return

        val queriedFoodResult = FoodHelper.query(hoveredStack, player) ?: return

        val defaultFood = queriedFoodResult.defaultFoodComponent
        val modifiedFood = queriedFoodResult.modifiedFoodComponent

        // Notify everyone that we should render tooltip overlay
        val prerenderEvent = TooltipOverlayEvent.Pre(hoveredStack, defaultFood, modifiedFood)
        TooltipOverlayEvent.Pre.EVENT.invoker().interact(prerenderEvent)
        if (prerenderEvent.isCanceled) return

        val foodOverlay = FoodOverlay(
            prerenderEvent.itemStack, defaultFood, modifiedFood,
            queriedFoodResult.consumableComponent, player,
        )
        if (foodOverlay.shouldRenderHungerBars()) {
            try {
                tooltip.add(FoodOverlayTextComponent(foodOverlay))
            } catch (_: UnsupportedOperationException) {
                // The list is immutable, e.g. the item has the HIDE_TOOLTIP component.
                // In addition to checking for that component, we catch this exception
                // just in case there are other reasons the list could be immutable.
            }
        }
    }

    enum class FoodOutline {
        NEGATIVE, EXTRA, NORMAL, PARTIAL, MISSING;

        fun argb(): Int = when (this) {
            NEGATIVE -> ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, 1.0f)
            EXTRA -> ColorHelper.argbFromRGBA(0.06f, 0.32f, 0.02f, 1.0f)
            NORMAL -> ColorHelper.argbFromRGBA(0.0f, 0.0f, 0.0f, 1.0f)
            PARTIAL -> ColorHelper.argbFromRGBA(0.53f, 0.21f, 0.08f, 1.0f)
            MISSING -> ColorHelper.argbFromRGBA(0.62f, 0.0f, 0.0f, 0.5f)
        }

        companion object {
            fun get(modifiedFoodHunger: Int, defaultFoodHunger: Int, i: Int): FoodOutline = when {
                modifiedFoodHunger < 0 -> NEGATIVE
                modifiedFoodHunger > defaultFoodHunger && defaultFoodHunger <= i -> EXTRA
                modifiedFoodHunger > i + 1 || defaultFoodHunger == modifiedFoodHunger -> NORMAL
                modifiedFoodHunger == i + 1 -> PARTIAL
                else -> MISSING
            }
        }
    }

    fun onRenderTooltip(
        context: GuiGraphicsExtractor, foodOverlay: FoodOverlay, toolTipX: Int, toolTipY: Int,
        textRenderer: Font,
    ) {
        val matrixStack: Matrix3x2fStack
        var itemStack = foodOverlay.itemStack

        val defaultFood = foodOverlay.defaultFood
        val modifiedFood = foodOverlay.modifiedFood

        var x = toolTipX
        var y = toolTipY

        // Notify everyone that we should render tooltip overlay
        val renderEvent = TooltipOverlayEvent.Render(
            itemStack, x, y, context, defaultFood, modifiedFood,
        )
        TooltipOverlayEvent.Render.EVENT.invoker().interact(renderEvent)
        if (renderEvent.isCanceled) return

        x = renderEvent.x
        y = renderEvent.y

        val graphics = renderEvent.context
        itemStack = renderEvent.itemStack
        matrixStack = graphics.pose()

        val defaultFoodHunger = defaultFood.nutrition()
        val modifiedFoodHunger = modifiedFood.nutrition()

        // Render from right to left so that the icons 'face' the right way
        x += (foodOverlay.hungerBars - 1) * 9

        val isRotten = FoodHelper.isRotten(foodOverlay.consumableComponent)

        var i = 0
        while (i < foodOverlay.hungerBars * 2) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.FOOD_EMPTY_TEXTURE, x, y, 9, 9)

            val outline = FoodOutline.get(modifiedFoodHunger, defaultFoodHunger, i)
            if (outline != FoodOutline.NORMAL) {
                graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, TextureHelper.HUNGER_OUTLINE_SPRITE, x, y, 9, 9, outline.argb(),
                )
            }

            val isDefaultHalf = defaultFoodHunger - 1 == i
            val defaultFoodIcon = TextureHelper.getFoodTexture(isRotten, if (isDefaultHalf) FoodType.HALF else FoodType.FULL)
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, defaultFoodIcon, x, y, 9, 9,
                ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, 0.25f),
            )

            if (modifiedFoodHunger > i) {
                val isModifiedHalf = modifiedFoodHunger - 1 == i
                val modifiedFoodIcon = TextureHelper.getFoodTexture(isRotten, if (isModifiedHalf) FoodType.HALF else FoodType.FULL)
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, modifiedFoodIcon, x, y, 9, 9)
            }

            x -= 9
            i += 2
        }
        foodOverlay.hungerBarsText?.let { text ->
            x += 18
            matrixStack.pushMatrix()
            matrixStack.translate(x.toFloat(), y.toFloat())
            matrixStack.scale(0.75f, 0.75f)
            graphics.text(textRenderer, text, 2, 2, 0xFFAAAAAA.toInt())
            matrixStack.popMatrix()
        }

        x = toolTipX
        y += 10

        val modifiedSaturationIncrement = modifiedFood.saturation()
        val absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement)

        // Render from right to left so that the icons 'face' the right way
        x += (foodOverlay.saturationBars - 1) * 7

        i = 0
        while (i < foodOverlay.saturationBars * 2) {
            val effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f

            val shouldBeFaded = absModifiedSaturationIncrement <= i
            val color =
                if (shouldBeFaded) ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, 0.5f)
                else ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, 1.0f)
            graphics.blit(
                RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, x, y,
                (if (effectiveSaturationOfBar >= 1) 21 else if (effectiveSaturationOfBar > 0.5) 14 else if (effectiveSaturationOfBar > 0.25) 7 else if (effectiveSaturationOfBar > 0) 0 else 28).toFloat(),
                (if (modifiedSaturationIncrement >= 0) 27 else 34).toFloat(),
                7, 7, 256, 256, color,
            )

            x -= 7
            i += 2
        }
        foodOverlay.saturationBarsText?.let { text ->
            x += 14
            matrixStack.pushMatrix()
            matrixStack.translate(x.toFloat(), y.toFloat())
            matrixStack.scale(0.75f, 0.75f)
            graphics.text(textRenderer, text, 2, 1, 0xFFAAAAAA.toInt())
            matrixStack.popMatrix()
        }
    }

    private fun shouldShowTooltip(hoveredStack: ItemStack, type: TooltipFlag): Boolean {
        if (hoveredStack.isEmpty) {
            return false
        }

        // Note: The intention here is to match the logic in ItemStack.getTooltip
        if (!type.isCreative && hoveredStack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).hideTooltip()) {
            return false
        }

        val shouldShow = (ModConfig.showFoodValuesInTooltip && KeyHelper.isShiftKeyDown()) ||
            ModConfig.showFoodValuesInTooltipAlways
        if (!shouldShow) {
            return false
        }

        if (!FoodHelper.isFood(hoveredStack)) {
            return false
        }

        return true
    }
}
