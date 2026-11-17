package squeek.appleskin.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.ohmyloader.api.core.hook.At;
import org.ohmyloader.api.core.hook.Inject;
import org.ohmyloader.api.core.hook.Hooked;
import org.ohmyloader.api.core.hook.HookValue;
import org.ohmyloader.api.core.hook.LocalCapture;
import squeek.appleskin.client.TooltipOverlayHandler;

/**
 * Lets AppleSkin's own {@link TooltipOverlayHandler.FoodOverlay} component ride the game's tooltip
 * pipeline: when the screen converts a text line into a renderable component, the overlay line is
 * recognized here and swapped for the component that draws the hunger icons.
 *
 * OML shape notes: the target is an **interface** and the anchors are its **static** factories, so
 * this mixin is an injection-only carrier (nothing to merge) and the handlers are static — the
 * engine dispatches them through the generated bridge with no receiver.
 */
@Hooked(target = "net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent")
public class ClientTooltipComponentMixin {

    @Inject(
        method = "create",
        desc = "(Lnet/minecraft/util/FormattedCharSequence;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
        at = {@At(value = At.HEAD)}, cancelable = true, require = 1,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public static void appleskinOf(FormattedCharSequence text, HookValue<ClientTooltipComponent> cir) {
        if (text instanceof TooltipOverlayHandler.FoodOverlayTextComponent) {
            cir.setReturnValue(((TooltipOverlayHandler.FoodOverlayTextComponent) text).getFoodOverlay());
        }
    }

    @Inject(
        method = "create",
        desc = "(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
        at = {@At(value = At.HEAD)}, cancelable = true, require = 1,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public static void appleskinOfData(TooltipComponent data, HookValue<ClientTooltipComponent> cir) {
        if (data instanceof TooltipOverlayHandler.FoodOverlay) {
            cir.setReturnValue((ClientTooltipComponent) data);
        }
    }
}
