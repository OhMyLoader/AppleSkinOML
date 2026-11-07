package squeek.appleskin.mixin;

import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import org.ohmyloader.api.hook.Hooked;
import org.ohmyloader.api.hook.MethodAccess;

/**
 * Static access to `DebugScreenEntries.register` — private in the game, and the one member the F3
 * food-stats line needs. The static accessor shape: the carrier declares a static method **with a
 * body** (the JVM forbids `static abstract` on interfaces); the engine synthesizes a public static
 * delegate into the target class and replaces this body with a call to it at carrier load.
 *
 * Written in Java for the same reason as the other static carriers: Kotlin companion delegates do
 * not carry the annotations the scanner reads.
 */
@Hooked(target = "net.minecraft.client.gui.components.debug.DebugScreenEntries")
public interface DebugScreenEntriesAccess {

    @MethodAccess("register")
    static Identifier register(Identifier id, DebugScreenEntry entry) {
        throw new AssertionError("replaced by the hook engine");
    }
}
