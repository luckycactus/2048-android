package ru.luckycactus.game2048view

import android.content.res.Resources
import android.util.TypedValue
import kotlin.math.floor

fun dpF(dp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        Resources.getSystem().displayMetrics
    )
}

fun dp(dp: Float): Int {
    return floor(dpF(dp).toDouble()).toInt()
}

fun spF(sp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        Resources.getSystem().displayMetrics
    )
}

fun sp(sp: Float): Int {
    return floor(spF(sp).toDouble()).toInt()
}

fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

fun isPowerOfTwo(x: Int) = (x != 0) && ((x and (x - 1)) == 0)