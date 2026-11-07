package squeek.appleskin.helpers

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.Window
import net.minecraft.client.Minecraft
import net.minecraft.util.Util

object KeyHelper {

    fun isCtrlKeyDown(): Boolean {
        // prioritize CONTROL, but allow OPTION as well on Mac (note: GuiScreen's isCtrlKeyDown only checks for the OPTION key on Mac)
        var ctrl = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) ||
            InputConstants.isKeyDown(InputConstants.KEY_RCONTROL)
        if (!ctrl && Util.getPlatform() == Util.OS.OSX) {
            ctrl = InputConstants.isKeyDown(InputConstants.KEY_LGUI) ||
                InputConstants.isKeyDown(InputConstants.KEY_RGUI)
        }
        return ctrl
    }

    fun isShiftKeyDown(): Boolean {
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) ||
            InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)
    }
}
