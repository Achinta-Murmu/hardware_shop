package com.galla.app.core.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Quantities are whole "thousandths of a unit" in a Long, so 2.5 m of wire is 2500.
 * That lets a shop sell half a kilo or 2.5 metres without using decimal numbers,
 * which would slowly go wrong when added up.
 */
object Qty {
    const val ONE = 1000L

    /** 2500 becomes "2.5", 3000 becomes "3". */
    fun format(milli: Long): String =
        if (milli % ONE == 0L) (milli / ONE).toString()
        else BigDecimal(milli).movePointLeft(3).stripTrailingZeros().toPlainString()

    /** "2.5 m", "3 pcs". */
    fun withUnit(milli: Long, unit: String): String = "${format(milli)} $unit"

    /** What was typed, as thousandths. Null if it is not a number, is negative, or has a fraction where none is allowed. */
    fun parse(text: String, allowFraction: Boolean): Long? {
        val cleaned = text.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val number = cleaned.toBigDecimalOrNull() ?: return null
        if (number.signum() < 0) return null
        if (number > BigDecimal("99999999")) return null
        val milli = number.movePointRight(3).setScale(0, RoundingMode.HALF_UP).toLong()
        if (!allowFraction && milli % ONE != 0L) return null
        return milli
    }

    /** Price per unit times quantity, rounded to the nearest paisa. */
    fun times(paisePerUnit: Long, milli: Long): Long {
        val product = paisePerUnit * milli
        return if (product >= 0) (product + ONE / 2) / ONE else -((-product + ONE / 2) / ONE)
    }
}
