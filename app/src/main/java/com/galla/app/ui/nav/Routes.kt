package com.galla.app.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every screen has a "key": a small object that says which screen it is and carries the
 * few values that screen needs (an item id, a bill id). The back stack is simply a list of
 * these keys; the screen on top of the list is the one on display.
 */
@Serializable
sealed interface Route : NavKey

// ---- the four tabs ----
@Serializable data object Today : Route
@Serializable data object Sell : Route
@Serializable data object Items : Route
/** The Money tab. (Named MoneyTab because `Money` is already the helper that formats rupees.) */
@Serializable data object MoneyTab : Route

// ---- items ----
@Serializable data class ItemDetail(val itemId: Long) : Route

/** `itemId` 0 means "add a new item". */
@Serializable data class ItemEdit(val itemId: Long = 0) : Route

/** Stock arriving from a supplier. `itemId` 0 means "let me pick the item first". */
@Serializable data class StockIn(val itemId: Long = 0) : Route

/** "I counted the shelf": set the real stock of one item. */
@Serializable data class StockCount(val itemId: Long) : Route

@Serializable data object Restock : Route

// ---- selling ----
/** The camera. `mode` is "sell" (scanned items go on the bill) or "find" (open the item). */
@Serializable data class Scan(val mode: String) : Route

/** A saved bill. `fresh` is true right after the sale, to show the "done" moment. */
@Serializable data class Receipt(val saleId: Long, val fresh: Boolean = false) : Route

// ---- money and settings ----
@Serializable data object ExpenseAdd : Route
@Serializable data object SettingsScreenKey : Route

val TOP_LEVEL: List<Route> = listOf(Today, Sell, Items, MoneyTab)
