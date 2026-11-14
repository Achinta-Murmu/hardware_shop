package com.galla.app.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galla.app.AppContainer
import com.galla.app.core.util.Dates
import com.galla.app.core.util.TimeRange
import com.galla.app.data.local.db.ExpenseEntity
import com.galla.app.data.local.db.PurchaseEntity
import com.galla.app.data.local.db.SaleEntity
import com.galla.app.ui.components.Bar
import com.galla.app.ui.components.asState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class Period { DAY, MONTH }

/** An item and how much of it was sold in the period. */
data class TopSeller(val name: String, val totalPaise: Long, val profitPaise: Long)

/** Everything the Money tab shows. */
data class MoneyUi(
    val loaded: Boolean = false,
    val period: Period = Period.DAY,
    /** "Today", "Yesterday", "12 Nov" or "November 2026". */
    val title: String = "",
    /** False when the period shown is the current one (there is no "next"). */
    val canGoForward: Boolean = false,
    /** Money in from sales. */
    val incomePaise: Long = 0,
    /** What the goods that were sold had cost the shop. */
    val costOfGoodsPaise: Long = 0,
    /** Rent, electricity and other running costs. */
    val expensesPaise: Long = 0,
    /** Money paid to suppliers for stock. */
    val purchasesPaise: Long = 0,
    val bills: List<SaleEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val topSellers: List<TopSeller> = emptyList(),
    val chartTitle: String = "",
    val bars: List<Bar> = emptyList(),
    val now: Long = 0,
) {
    /** Profit = sales, minus what those goods cost, minus running costs. */
    val profitPaise: Long get() = incomePaise - costOfGoodsPaise - expensesPaise

    /** All money that left the shop: stock bought plus running costs. */
    val moneyOutPaise: Long get() = purchasesPaise + expensesPaise
}

private data class Window(val period: Period, val anchor: LocalDate, val today: LocalDate) {
    val range: TimeRange get() = if (period == Period.DAY) Dates.day(anchor) else Dates.month(YearMonth.from(anchor))

    /** The chart shows the seven days ending on the chosen day, or every day of the chosen month. */
    val chartDays: List<LocalDate>
        get() = if (period == Period.DAY) {
            (6 downTo 0).map { anchor.minusDays(it.toLong()) }
        } else {
            val month = YearMonth.from(anchor)
            (1..month.lengthOfMonth()).map { month.atDay(it) }
        }
}

/**
 * ViewModel for the Money tab: income, spending and profit for one day or one month.
 * Nothing is stored for this screen. It is all worked out from the bills, purchases and
 * expenses in the database, so it can never disagree with them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoneyViewModel(private val app: AppContainer) : ViewModel() {

    private val period = MutableStateFlow(Period.DAY)
    private val anchor = MutableStateFlow(Dates.dateOf(app.now()))

    private val window = combine(period, anchor) { period, anchor -> Window(period, anchor, Dates.dateOf(app.now())) }

    val state: StateFlow<MoneyUi> = window.flatMapLatest { window ->
        val range = window.range
        val days = window.chartDays
        combine(
            app.shop.sales(range),
            app.shop.expenses(range),
            app.shop.purchases(range),
            app.shop.saleLines(range),
            app.shop.sales(Dates.days(days.first(), days.last())),
        ) { sales, expenses, purchases, lines, chartSales ->
            val perDay = chartSales.groupBy { Dates.dateOf(it.at) }.mapValues { (_, list) -> list.sumOf { it.totalPaise } }
            MoneyUi(
                loaded = true,
                period = window.period,
                title = if (window.period == Period.DAY) Dates.friendlyDay(window.anchor, window.today) else Dates.monthYear(YearMonth.from(window.anchor)),
                canGoForward = if (window.period == Period.DAY) window.anchor < window.today else YearMonth.from(window.anchor) < YearMonth.from(window.today),
                incomePaise = sales.sumOf { it.totalPaise },
                costOfGoodsPaise = sales.sumOf { it.costPaise },
                expensesPaise = expenses.sumOf { it.amountPaise },
                purchasesPaise = purchases.sumOf { it.totalPaise },
                bills = sales,
                expenses = expenses,
                purchases = purchases,
                topSellers = lines.groupBy { it.name }
                    .map { (name, list) -> TopSeller(name, list.sumOf { it.totalPaise }, list.sumOf { it.totalPaise - it.totalCostPaise }) }
                    .sortedByDescending { it.totalPaise }
                    .take(5),
                chartTitle = if (window.period == Period.DAY) "Sales, 7 days to ${Dates.dayMonth(window.anchor)}" else "Sales, day by day",
                bars = days.map { day ->
                    Bar(
                        label = if (window.period == Period.DAY) Dates.weekday(day) else day.dayOfMonth.toString(),
                        value = perDay[day] ?: 0L,
                        highlight = if (window.period == Period.DAY) day == window.anchor else day == window.today,
                    )
                },
                now = app.now(),
            )
        }
    }.asState(this, MoneyUi())

    fun setPeriod(choice: Period) {
        period.value = choice
    }

    /** Moves one day or one month back (-1) or forward (+1). Never into the future. */
    fun step(by: Int) {
        val today = Dates.dateOf(app.now())
        val next = if (period.value == Period.DAY) anchor.value.plusDays(by.toLong()) else anchor.value.plusMonths(by.toLong())
        anchor.value = if (next > today) today else next
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch { app.shop.deleteExpense(id) }
    }
}
