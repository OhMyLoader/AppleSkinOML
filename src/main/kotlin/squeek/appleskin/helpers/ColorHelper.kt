package squeek.appleskin.helpers

import net.minecraft.util.Mth

object ColorHelper {
    fun argbFromRGBA(r: Float, g: Float, b: Float, a: Float): Int =
        (Mth.floor((a * 255.0).toFloat()) shl 24) or
                (Mth.floor((r * 255.0).toFloat()) shl 16) or
                (Mth.floor((g * 255.0).toFloat()) shl 8) or
                Mth.floor((b * 255.0).toFloat())
}
