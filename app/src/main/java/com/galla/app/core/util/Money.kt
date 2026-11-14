package com.galla.app.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Money is always a whole number of paise (100 paise = 1 rupee), held in a Long.
 * Never a decimal number: decimals drift by a paisa when you add many of them up.
 */
object Money {

    /** 1234550 paise becomes "₹12,345.50". Whole rupees drop the ".00": "₹12,345". */
    fun format(paise: Long, symbol: Boolean = true): String {
        val value = abs(paise)
        val rupees = value / 100
        val rest = (value % 100).toInt()
        val out = StringBuilder()
        if (paise < 0) out.append('-')
        if (symbol) out.append('₹')
        out.append(groupIndian(rupees))
        if (rest != 0) out.append('.').append(rest.toString().padStart(2, '0'))
        return out.toString()
    }

    /** Indian digit grouping: the last three digits, then pairs. 1234567 becomes "12,34,567". */
    fun groupIndian(number: Long): String {
        val digits = number.toString()
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        var head = digits.dropLast(3)
        val pairs = ArrayList<String>()
        while (head.length > 2) {
            pairs.add(head.takeLast(2))
            head = head.dropLast(2)
        }
        if (head.isNotEmpty()) pairs.add(head)
        return pairs.reversed().joinToString(",") + "," + lastThree
    }

    /** A short form for chart labels: "₹950", "₹12.5k", "₹3.2L", "₹1.4Cr". */
    fun compact(paise: Long): String {
        val rupees = abs(paise) / 100.0
        val sign = if (paise < 0) "-" else ""
        fun one(value: Double): String {
            val text = String.format(java.util.Locale.US, "%.1f", value)
            return if (text.endsWith(".0")) text.dropLast(2) else text
        }
        return when {
            rupees >= 10_000_000 -> "$sign₹${one(rupees / 10_000_000)}Cr"
            rupees >= 100_000 -> "$sign₹${one(rupees / 100_000)}L"
            rupees >= 1_000 -> "$sign₹${one(rupees / 1_000)}k"
            else -> "$sign₹${rupees.toLong()}"
        }
    }

    /** What the shop owner typed ("12.5", "1,200") as paise, or null if it is not a valid amount. */
    fun parse(text: String): Long? {
        val cleaned = text.trim().replace(",", "").removePrefix("₹").trim()
        if (cleaned.isEmpty()) return null
        val number = cleaned.toBigDecimalOrNull() ?: return null
        if (number.signum() < 0) return null
        if (number > BigDecimal("9999999999")) return null
        return number.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    /** Paise as plain text for an input box: 1250 becomes "12.5", 1200 becomes "12". */
    fun plain(paise: Long): String =
        if (paise % 100 == 0L) (paise / 100).toString()
        else BigDecimal(paise).movePointLeft(2).stripTrailingZeros().toPlainString()
}
