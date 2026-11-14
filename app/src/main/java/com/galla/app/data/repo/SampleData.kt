package com.galla.app.data.repo

import com.galla.app.core.model.StockLevel
import com.galla.app.core.model.Units
import com.galla.app.core.util.Dates
import com.galla.app.core.util.Qty
import kotlin.random.Random

/**
 * A made-up hardware shop with a week of trade, so the app can be tried before any real
 * item is typed in. It goes through the same repository as real use, so the sample
 * stock ledger, bills and profit are all consistent.
 */
class SampleData(private val shop: ShopRepository, private val now: () -> Long) {

    private data class Sample(
        val name: String, val category: String, val unit: String,
        val cost: Double, val price: Double, val stock: Double, val min: Double,
    )

    private val samples = listOf(
        Sample("Emulsion Paint 1 L (White)", "Paint", "can", 210.0, 265.0, 14.0, 4.0),
        Sample("Paint Brush 2 inch", "Paint", "pcs", 32.0, 55.0, 26.0, 8.0),
        Sample("Wood Adhesive 500 g", "Adhesive", "pcs", 118.0, 150.0, 22.0, 6.0),
        Sample("Epoxy Putty 100 g", "Adhesive", "pcs", 38.0, 55.0, 17.0, 6.0),
        Sample("Switch 6A", "Electrical", "pcs", 18.0, 28.0, 120.0, 30.0),
        Sample("Copper Wire 1.5 sq mm", "Electrical", "m", 14.5, 21.0, 180.0, 50.0),
        Sample("LED Bulb 9W", "Electrical", "pcs", 62.0, 95.0, 3.0, 10.0),
        Sample("CPVC Pipe 1/2 inch", "Plumbing", "ft", 16.0, 24.0, 240.0, 60.0),
        Sample("Brass Bib Tap", "Plumbing", "pcs", 135.0, 210.0, 9.0, 4.0),
        Sample("Teflon Tape", "Plumbing", "roll", 8.0, 15.0, 0.0, 20.0),
        Sample("Wire Nails 2 inch", "Fasteners", "kg", 68.0, 95.0, 42.0, 10.0),
        Sample("Wood Screws 1 inch (box of 100)", "Fasteners", "box", 45.0, 70.0, 31.0, 8.0),
        Sample("Screwdriver Set (6 pc)", "Tools", "set", 240.0, 340.0, 6.0, 2.0),
        Sample("Measuring Tape 5 m", "Tools", "pcs", 150.0, 220.0, 2.0, 3.0),
        Sample("Door Hinge 4 inch", "Fittings", "pair", 48.0, 80.0, 34.0, 10.0),
        Sample("Sandpaper Sheet 120", "Fittings", "sheet", 6.0, 12.0, 150.0, 40.0),
    )

    private fun paise(rupees: Double): Long = Math.round(rupees * 100)
    private fun milli(units: Double): Long = Math.round(units * Qty.ONE)

    suspend fun load() {
        val current = now()
        val today = Dates.dateOf(current)
        val random = Random(42)
        val hour = 3_600_000L

        val ids = samples.map { s ->
            shop.saveItem(
                ItemDraft(
                    name = s.name, category = s.category, unit = s.unit,
                    costPaise = paise(s.cost), pricePaise = paise(s.price),
                    minStockMilli = milli(s.min), openingStockMilli = milli(s.stock),
                ),
                at = Dates.startOf(today.minusDays(7)) + 9 * hour,
            )
        }

        // A week of trade, oldest day first.
        for (daysAgo in 6 downTo 0) {
            val dayStart = Dates.startOf(today.minusDays(daysAgo.toLong()))
            val bills = if (daysAgo == 0) 3 else 3 + random.nextInt(4)
            repeat(bills) { index ->
                val at = if (daysAgo == 0) {
                    // Today's bills must not be in the future.
                    (current - (bills - index) * 35 * 60_000L).coerceAtLeast(dayStart)
                } else {
                    dayStart + 10 * hour + index * hour + random.nextInt(50) * 60_000L
                }
                val bill = LinkedHashMap<Long, Long>()
                repeat(1 + random.nextInt(3)) {
                    val id = ids[random.nextInt(ids.size)]
                    val item = shop.getItem(id) ?: return@repeat
                    // Leave the low and empty shelves alone so the restock list has something to show.
                    if (item.level != StockLevel.OK) return@repeat
                    val wanted = if (Units.of(item.unit).fractional) milli(1.0 + random.nextInt(6) * 0.5) else milli(1.0 + random.nextInt(3))
                    if (wanted <= item.stockMilli - item.minStockMilli) bill[id] = wanted
                }
                if (bill.isNotEmpty()) shop.completeSale(bill, at)
            }
        }

        // Some stock bought and some shop costs paid during the week.
        shop.stockIn(ids[4], milli(50.0), paise(18.0), "Gupta Electricals", Dates.startOf(today.minusDays(4)) + 12 * hour)
        shop.stockIn(ids[10], milli(20.0), paise(68.0), "Shree Traders", Dates.startOf(today.minusDays(2)) + 11 * hour)
        shop.addExpense("Electricity", "Monthly bill", paise(1850.0), Dates.startOf(today.minusDays(5)) + 13 * hour)
        shop.addExpense("Transport", "Delivery van", paise(300.0), Dates.startOf(today.minusDays(3)) + 16 * hour)
        shop.addExpense("Tea & snacks", "", paise(120.0), Dates.startOf(today.minusDays(1)) + 17 * hour)
        shop.addExpense("Tea & snacks", "", paise(80.0), (current - 20 * 60_000L).coerceAtLeast(Dates.startOf(today)))
    }
}
