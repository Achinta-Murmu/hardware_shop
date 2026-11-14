package com.galla.app.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.galla.app.core.util.Dates
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.ui.components.AmountRow
import com.galla.app.ui.components.BarChart
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnStyle
import com.galla.app.ui.components.ButtonGroup
import com.galla.app.ui.components.CardKind
import com.galla.app.ui.components.Chevron
import com.galla.app.ui.components.Eyebrow
import com.galla.app.ui.components.GCard
import com.galla.app.ui.components.GGroup
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.GroupItem
import com.galla.app.ui.components.GroupOption
import com.galla.app.ui.components.HeroCard
import com.galla.app.ui.components.IconBtn
import com.galla.app.ui.components.IconBtnStyle
import com.galla.app.ui.components.RollingText
import com.galla.app.ui.components.RowContent
import com.galla.app.ui.components.ScreenColumn
import com.galla.app.ui.components.SectionHeader
import com.galla.app.ui.components.ShapeIcon
import com.galla.app.ui.components.StatTile
import com.galla.app.ui.components.TabHeader
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.ExpenseAdd
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalMessenger
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.nav.Receipt
import com.galla.app.ui.theme.GallaTheme
import com.galla.app.ui.theme.GallaType

/**
 * The Money tab: what came in, what went out, and what is left, for a day or a month.
 * The big number is profit, because that is the question an owner actually has.
 */
@Composable
fun MoneyScreen() {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val messenger = LocalMessenger.current
    val vm: MoneyViewModel = viewModel { MoneyViewModel(app) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val loss = ui.profitPaise < 0L

    ScreenColumn {
        TabHeader(
            title = "Money",
            eyebrow = "In, out and profit",
            actions = { IconBtn(Ic.add, "Add an expense", { nav.go(ExpenseAdd) }, style = IconBtnStyle.TONAL) },
        )

        ButtonGroup(
            options = listOf(GroupOption("Day", Ic.calendar_today), GroupOption("Month", Ic.calendar_month)),
            selectedIndex = if (ui.period == Period.DAY) 0 else 1,
            onSelect = { vm.setPeriod(if (it == 0) Period.DAY else Period.MONTH) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // Which day or month
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(Ic.chevron_left, "Earlier", { vm.step(-1) })
            Text(ui.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconBtn(Ic.chevron_right, "Later", { vm.step(1) }, enabled = ui.canGoForward)
        }
        Gap(4.dp)

        // The hero: profit
        HeroCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            kind = if (loss) CardKind.ERROR else CardKind.HERO,
        ) {
            val onCard = if (loss) scheme.onErrorContainer else GallaTheme.extras.onHero
            Eyebrow(if (loss) "Loss" else "Profit", color = onCard.copy(alpha = 0.72f))
            Gap(2.dp)
            val profit = Money.format(if (loss) -ui.profitPaise else ui.profitPaise)
            RollingText(profit, style = fitted(profit))
            Gap(4.dp)
            Text("Sales, minus what the goods cost, minus expenses.", style = MaterialTheme.typography.bodyMedium)
        }
        Gap(10.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile("Money in", Money.format(ui.incomePaise), Modifier.weight(1f), icon = Ic.trending_up)
            StatTile("Money out", Money.format(ui.moneyOutPaise), Modifier.weight(1f), kind = CardKind.INK, icon = Ic.trending_down)
        }
        Gap(10.dp)

        // Sales chart
        GCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Eyebrow(ui.chartTitle)
            Gap(14.dp)
            BarChart(ui.bars, description = ui.chartTitle)
        }
        Gap(10.dp)

        // How the profit is made up
        GCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Eyebrow("How it adds up")
            Gap(8.dp)
            AmountRow("Sales (${ui.bills.size} ${if (ui.bills.size == 1) "bill" else "bills"})", Money.format(ui.incomePaise))
            AmountRow("What those goods cost", "-" + Money.format(ui.costOfGoodsPaise))
            AmountRow("Expenses", "-" + Money.format(ui.expensesPaise))
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = scheme.outlineVariant)
            AmountRow(if (loss) "Loss" else "Profit", Money.format(if (loss) -ui.profitPaise else ui.profitPaise), strong = true, color = if (loss) scheme.error else GallaTheme.extras.good)
            Gap(8.dp)
            Text(
                "Stock bought: ${Money.format(ui.purchasesPaise)}. It counts as money out, but only becomes a cost in profit when it is sold.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }

        // Best sellers
        if (ui.topSellers.isNotEmpty()) {
            SectionHeader("Best sellers")
            GGroup {
                ui.topSellers.forEachIndexed { index, seller ->
                    GroupItem(index, ui.topSellers.size) {
                        RowContent(
                            title = seller.name,
                            subtitle = "${Money.format(seller.profitPaise)} profit",
                            leading = {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = scheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                )
                            },
                            trailing = { Text(Money.format(seller.totalPaise), style = GallaType.amount) },
                        )
                    }
                }
            }
        }

        // Bills
        SectionHeader("Bills")
        if (ui.bills.isEmpty()) {
            Quiet(if (ui.loaded) "No sales in this period." else "")
        } else {
            val shown = ui.bills.take(40)
            GGroup {
                shown.forEachIndexed { index, bill ->
                    GroupItem(index, shown.size, onClick = { nav.go(Receipt(bill.id)) }) {
                        RowContent(
                            title = "Bill no. ${bill.id}",
                            subtitle = "${bill.lineCount} ${if (bill.lineCount == 1) "item" else "items"} · ${Dates.friendly(bill.at, ui.now)}",
                            leading = { ShapeIcon(Ic.receipt_long, size = 40.dp) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(Money.format(bill.totalPaise), style = GallaType.amount)
                                    Chevron()
                                }
                            },
                        )
                    }
                }
            }
            if (ui.bills.size > shown.size) Quiet("Showing the latest ${shown.size} of ${ui.bills.size} bills.")
        }

        // Expenses
        SectionHeader("Expenses", link = "Add", onLink = { nav.go(ExpenseAdd) })
        if (ui.expenses.isEmpty()) {
            Quiet(if (ui.loaded) "No expenses in this period." else "")
        } else {
            GGroup {
                ui.expenses.forEachIndexed { index, expense ->
                    GroupItem(index, ui.expenses.size) {
                        RowContent(
                            title = expense.category,
                            subtitle = listOf(expense.note, Dates.friendly(expense.at, ui.now)).filter { it.isNotBlank() }.joinToString(" · "),
                            leading = { ShapeIcon(Ic.payments, size = 40.dp, background = scheme.surfaceContainerHighest, tint = scheme.onSurfaceVariant) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(Money.format(expense.amountPaise), style = GallaType.amount)
                                    IconBtn(
                                        Ic.delete,
                                        "Delete this expense",
                                        {
                                            vm.deleteExpense(expense.id)
                                            messenger.show("Expense deleted")
                                        },
                                        size = 40.dp,
                                        iconSize = 20.dp,
                                        tint = scheme.onSurfaceVariant,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        // Stock bought
        if (ui.purchases.isNotEmpty()) {
            SectionHeader("Stock bought")
            GGroup {
                ui.purchases.forEachIndexed { index, purchase ->
                    GroupItem(index, ui.purchases.size) {
                        RowContent(
                            title = purchase.itemName,
                            subtitle = listOf(Qty.withUnit(purchase.qtyMilli, purchase.unit), purchase.supplier, Dates.friendly(purchase.at, ui.now))
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            leading = { ShapeIcon(Ic.local_shipping, size = 40.dp, background = scheme.surfaceContainerHighest, tint = scheme.onSurfaceVariant) },
                            trailing = { Text(Money.format(purchase.totalPaise), style = GallaType.amount) },
                        )
                    }
                }
            }
        }

        Gap(16.dp)
        Btn(
            "Add an expense",
            { nav.go(ExpenseAdd) },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            icon = Ic.add,
            style = BtnStyle.TONAL,
        )
    }
}

/** A quiet line of text where a list would be. */
@Composable
private fun Quiet(text: String) {
    if (text.isEmpty()) return
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

/** The big number shrinks a little when the amount is long, so it always fits on one line. */
private fun fitted(text: String) = when {
    text.length <= 7 -> GallaType.hero
    text.length <= 9 -> GallaType.hero.copy(fontSize = GallaType.hero.fontSize * 0.82f, lineHeight = GallaType.hero.lineHeight * 0.82f)
    text.length <= 11 -> GallaType.hero.copy(fontSize = GallaType.hero.fontSize * 0.68f, lineHeight = GallaType.hero.lineHeight * 0.68f)
    else -> GallaType.hero.copy(fontSize = GallaType.hero.fontSize * 0.56f, lineHeight = GallaType.hero.lineHeight * 0.56f)
}
