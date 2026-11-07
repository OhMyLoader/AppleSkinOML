package squeek.appleskin.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer
import net.minecraft.client.gui.components.debug.DebugScreenEntry
import net.minecraft.resources.Identifier
import net.minecraft.world.food.FoodData
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import squeek.appleskin.helpers.ExhaustionHelper
import squeek.appleskin.helpers.FoodHelper
import java.text.DecimalFormat

/** The F3 food-stats line, ported as-is; the registration call lives in [squeek.appleskin.AppleSkinMod]. */
class DebugInfoHudEntry : DebugScreenEntry {

    override fun display(
        lines: DebugScreenDisplayer,
        world: Level?,
        clientChunk: LevelChunk?,
        chunk: LevelChunk?,
    ) {
        if (world != null) {
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return

            val stats: FoodData = player.foodData

            val curExhaustion = ExhaustionHelper.getExhaustion(player)
            val maxExhaustion = FoodHelper.MAX_EXHAUSTION
            lines.addToGroup(
                SECTION_ID,
                "hunger: " + stats.foodLevel + ", sat: " + saturationDF.format(stats.saturationLevel.toDouble()) +
                        ", exh: " + exhaustionValDF.format(curExhaustion.toDouble()) + "/" +
                        exhaustionMaxDF.format(maxExhaustion.toDouble()),
            )
        }
    }

    companion object {
        val ENTRY_ID: Identifier = Identifier.fromNamespaceAndPath("appleskin", "food_stats")
        val SECTION_ID: Identifier = Identifier.fromNamespaceAndPath("appleskin", "debug_info")

        private val saturationDF = DecimalFormat("#.##")
        private val exhaustionValDF = DecimalFormat("0.00")
        private val exhaustionMaxDF = DecimalFormat("#.##")
    }
}
