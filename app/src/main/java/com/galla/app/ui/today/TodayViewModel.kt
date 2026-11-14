package com.galla.app.ui.today

import androidx.lifecycle.ViewModel
import com.galla.app.AppContainer
import com.galla.app.core.model.StockLevel
import com.galla.app.core.util.Dates
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.data.local.db.ItemEntity
import com.galla.app.ui.components.asState
import com.galla.app.ui.icons.Ic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

/** One line of "Recent": a sale, some stock bought, or an expense. */
data class Activity(
    val key: String,
    val icon: Ic,
    val title: String,
    val subtitle: String,
    val amount: String,
    /** True when money came in. */
    val moneyIn: Boolean,
    val at: Long,
    /** The bill to open on tap, or 0. */
    val saleId: Long = 0,
)

/** Everything the Today screen shows. */
data class TodayUi(
    val loaded: Boolean = false,
    val shopTitle: String = "",
    val today: LocalDate = LocalDate.now(),
    val salesPaise: Long = 0,
    val bills: Int = 0,
    val profitPaise: Long = 0,
    val spentPaise: Long = 0,
    val stockValuePaise: Long = 0,
    val itemCount: Int = 0,
    /** Items that are low or sold out, most urgent first. */
    val restock: List<ItemEntity> = emptyList(),
    val activity: List<Activity> = emptyList(),
)

private data class DayTotals(val sales: Long, val bills: Int, val profit: Long, val spent: Long)

/**
 * ViewModel for the Today tab. It only reads: the database is watched, and this turns
 * what is in it into the numbers on the screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val app: AppContainer) : ViewModel() {

    /** Today's date, re-checked every half minute so the screen rolls over at midnight. */
    private val day: Flow<LocalDate> = flow {
        while (true) {
            emit(Dates.dateOf(app.now()))
            delay(30_000L)
        }
    }.distinctUntilChanged()

    private val totals: Flow<DayTotals> = day.flatMapLatest { date ->
        val range = Dates.day(date)
        combine(app.shop.sales(range), app.shop.expenses(range), app.shop.purchases(range)) { sales, expenses, purchases ->
            val expenseTotal = expenses.sumOf { it.amountPaise }
            DayTotals(
                sales = sales.sumOf { it.totalPaise },
                bills = sales.size,
                profit = sales.sumOf { it.totalPaise - it.costPaise } - expenseTotal,
                spent = expenseTotal + purchases.sumOf { it.totalPaise },
            )
        }
    }

    private val recent: Flow<List<Activity>> =
        combine(app.shop.recentSales(6), app.shop.recentPurchases(4), app.shop.recentExpenses(4)) { sales, purchases, expenses ->
            val now = app.now()
            val list = ArrayList<Activity>()
            sales.forEach {
                list.add(
                    Activity(
                        key = "s${it.id}", icon = Ic.receipt_long, title = "Bill no. ${it.id}",
                        subtitle = "${it.lineCount} ${if (it.lineCount == 1) "item" else "items"} · ${Dates.friendly(it.at, now)}",
                        amount = "+${Money.format(it.totalPaise)}", moneyIn = true, at = it.at, saleId = it.id,
                    ),
                )
            }
            purchases.forEach {
                list.add(
                    Activity(
                        key = "p${it.id}", icon = Ic.local_shipping, title = "Stock in: ${it.itemName}",
                        subtitle = "${Qty.withUnit(it.qtyMilli, it.unit)} · ${Dates.friendly(it.at, now)}",
                        amount = "-${Money.format(it.totalPaise)}", moneyIn = false, at = it.at,
                    ),
                )
            }
            expenses.forEach {
                list.add(
                    Activity(
                        key = "e${it.id}", icon = Ic.payments, title = it.category,
                        subtitle = listOf(it.note, Dates.friendly(it.at, now)).filter { text -> text.isNotBlank() }.joinToString(" · "),
                        amount = "-${Money.format(it.amountPaise)}", moneyIn = false, at = it.at,
                    ),
                )
            }
            list.sortedByDescending { it.at }.take(6)
        }

    val state: StateFlow<TodayUi> =
        combine(app.settings.settings, day, app.shop.items, totals, recent) { settings, date, items, totals, recent ->
            TodayUi(
                loaded = true,
                shopTitle = settings.shopTitle,
                today = date,
                salesPaise = totals.sales,
                bills = totals.bills,
                profitPaise = totals.profit,
                spentPaise = totals.spent,
                stockValuePaise = items.sumOf { it.stockValuePaise },
                itemCount = items.size,
                restock = items.filter { it.level != StockLevel.OK }
                    .sortedWith(compareBy({ it.level != StockLevel.OUT }, { it.stockMilli.toDouble() / (it.minStockMilli + 1) })),
                activity = recent,
            )
        }.asState(this, TodayUi())
}
