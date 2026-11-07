package squeek.appleskin.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.ohmyloader.api.hook.ModifyReturnValue;
import org.ohmyloader.api.hook.Merged;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

/**
 * The hook that offers the food overlay to every tooltip of every item: the RETURN-value modify runs
 * after the game has built its own lines, and the handler appends the overlay line in place. The
 * handler is merged into `ItemStack`, so `this` **is** the hovered stack.
 *
 * OML shape note: the merged `@ModifyReturnValue` handler receives only the return value — the
 * game's `(context, player, flag)` arguments are not passed. The two that matter are reconstructed
 * from the client itself: the hover path always uses the local player, and the flag it encodes is
 * exactly the advanced-tooltips toggle (F3+H), read from the same option vanilla builds the flag
 * from.
 */
@Merged(target = "net.minecraft.world.item.ItemStack")
public class ItemStackMixin {

    @ModifyReturnValue(
        method = "getTooltipLines",
        desc = "(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;" +
            "Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;",
        require = 1
    )
    public List<Component> onTooltip(List<Component> lines) {
        Player player = Minecraft.getInstance().player;
        TooltipFlag type = Minecraft.getInstance().options.advancedItemTooltips
            ? TooltipFlag.ADVANCED
            : TooltipFlag.NORMAL;
        TooltipOverlayHandler.onItemTooltip((ItemStack) (Object) this, player, type, lines);
        return lines;
    }
}
