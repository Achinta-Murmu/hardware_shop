package com.galla.app.core.model

/** A unit an item is counted in. `fractional` units can be sold in parts (2.5 m, 0.5 kg). */
data class UnitDef(val key: String, val label: String, val fractional: Boolean)

object Units {
    val all: List<UnitDef> = listOf(
        UnitDef("pcs", "Piece", false),
        UnitDef("box", "Box", false),
        UnitDef("pkt", "Packet", false),
        UnitDef("set", "Set", false),
        UnitDef("pair", "Pair", false),
        UnitDef("doz", "Dozen", false),
        UnitDef("roll", "Roll", false),
        UnitDef("bundle", "Bundle", false),
        UnitDef("bag", "Bag", false),
        UnitDef("sheet", "Sheet", false),
        UnitDef("bottle", "Bottle", false),
        UnitDef("can", "Can", false),
        UnitDef("kg", "Kilogram", true),
        UnitDef("g", "Gram", true),
        UnitDef("m", "Metre", true),
        UnitDef("cm", "Centimetre", true),
        UnitDef("ft", "Foot", true),
        UnitDef("in", "Inch", true),
        UnitDef("sqft", "Square foot", true),
        UnitDef("l", "Litre", true),
        UnitDef("ml", "Millilitre", true),
    )

    fun of(key: String): UnitDef = all.firstOrNull { it.key == key } ?: all.first()
}

/** Why an item's stock changed. Every change to stock is one row of one of these kinds. */
object MoveType {
    const val OPENING = "OPENING"
    const val PURCHASE = "PURCHASE"
    const val SALE = "SALE"
    const val ADJUST = "ADJUST"
}

/** What spending that is not stock gets filed under. */
object ExpenseCategories {
    val all: List<String> = listOf("Rent", "Electricity", "Salary", "Transport", "Tea & snacks", "Repairs", "Phone", "Other")
}

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/** Choices saved on the phone. There is no account: this is all the app "knows" about its owner. */
data class Settings(
    val onboarded: Boolean = false,
    val shopName: String = "",
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    /** Take the colours from the phone's wallpaper instead of Galla's own green. Android 12 or newer. */
    val dynamicColor: Boolean = false,
) {
    val shopTitle: String get() = shopName.ifBlank { "My shop" }
}

/** How full an item's shelf is. */
enum class StockLevel { OK, LOW, OUT }
