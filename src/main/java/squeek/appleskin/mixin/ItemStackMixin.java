package squeek.appleskin.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.ohmyloader.api.hook.ModifyReturnValue;
import org.ohmyloader.api.hook.Extra;
import org.ohmyloader.api.hook.Hooked;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

/**
 * The hook that offers the food overlay to every tooltip of every item: the RETURN-value modify runs
 * after the game has built its own lines, and the handler appends the overlay line in place. The
 * handler is merged into `ItemStack`, so `this` **is** the hovered stack (the Kotlin side reaches it
 * through a plain cast to the target type, which the merged body resolves against the instance).
 */
@Hooked(target = "net.minecraft.world.item.ItemStack")
public class ItemStackMixin {

    /**
     * Marks the mixin as **merge model** — without a merge-family member the scanner routes every
     * handler through the generated bridge, where `this` is the mixin instance and the cast below
     * throws ClassCastException (found by the first hover over an item).
     */
    @Extra
    private boolean mergedModel = true;

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
