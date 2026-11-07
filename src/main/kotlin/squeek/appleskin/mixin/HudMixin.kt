package squeek.appleskin.mixin

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.player.Player
import org.ohmyloader.api.mixin.At
import org.ohmyloader.api.mixin.Inject
import org.ohmyloader.api.mixin.LocalCapture
import org.ohmyloader.api.mixin.Mixin
import squeek.appleskin.client.HUDOverlayHandler

/**
 * The one mixin the HUD overlays cannot live without: the exact bar geometry (the food bar's
 * top/right and the hearts' full parameter list) exists only at these two `Hud` extraction passes,
 * and no OML event carries it. Descriptors are the 26.3 ones, pinned by the shape tests of the
 * loader engine (`require` turns a drift into a loud startup error, never a silent no-op).
 */
@Mixin(target = "net.minecraft.client.gui.Hud")
class HudMixin {

    @Inject(
        method = "extractFood",
        desc = "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;II)V",
        at = At(At.HEAD), require = 1,
        locals = LocalCapture.CAPTURE_FAILHARD,
    )
    fun renderFoodPre(
        context: GuiGraphicsExtractor,
        player: Player,
        top: Int,
        right: Int,
        ci: org.ohmyloader.api.mixin.CallbackInfo
    ) {
        HUDOverlayHandler.onPreRenderFood(context, player, top, right)
    }

    @Inject(
        method = "extractFood",
        desc = "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;II)V",
        at = At(At.RETURN), require = 1,
        locals = LocalCapture.CAPTURE_FAILHARD,
    )
    fun renderFoodPost(
        context: GuiGraphicsExtractor,
        player: Player,
        top: Int,
        right: Int,
        ci: org.ohmyloader.api.mixin.CallbackInfo
    ) {
        HUDOverlayHandler.onRenderFood(context, player, top, right)
    }

    @Inject(
        method = "extractHearts",
        desc = "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIIIIFFFIIIZ)V",
        at = At(At.RETURN), require = 1,
        locals = LocalCapture.CAPTURE_FAILHARD,
    )
    fun renderHealthPost(
        context: GuiGraphicsExtractor, player: Player, left: Int, top: Int, lines: Int,
        regeneratingHeartIndex: Int, maxHealth: Float, lastHealth: Int, health: Int, absorption: Int,
        blinking: Boolean, ci: org.ohmyloader.api.mixin.CallbackInfo,
    ) {
        HUDOverlayHandler.onRenderHealth(
            context, player, left, top, lines, regeneratingHeartIndex, maxHealth, lastHealth,
            health, absorption, blinking,
        )
    }
}
