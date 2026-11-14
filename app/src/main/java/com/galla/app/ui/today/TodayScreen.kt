package com.galla.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.galla.app.core.model.StockLevel
import com.galla.app.core.util.Dates
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnStyle
import com.galla.app.ui.components.CardKind
import com.galla.app.ui.components.Chevron
import com.galla.app.ui.components.Eyebrow
import com.galla.app.ui.components.Foot
import com.galla.app.ui.components.GCard
import com.galla.app.ui.components.GGroup
import com.galla.app.ui.components.GIcon
import com.galla.app.ui.components.GallaLogo
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.GroupItem
import com.galla.app.ui.components.HeroCard
import com.galla.app.ui.components.IconBtn
import com.galla.app.ui.components.RollingText
import com.galla.app.ui.components.RowContent
import com.galla.app.ui.components.ScreenColumn
import com.galla.app.ui.components.SectionHeader
import com.galla.app.ui.components.ShapeIcon
import com.galla.app.ui.components.StatTile
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.ExpenseAdd
import com.galla.app.ui.nav.ItemEdit
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.nav.MoneyTab
import com.galla.app.ui.nav.Receipt
import com.galla.app.ui.nav.Restock
import com.galla.app.ui.nav.Scan
import com.galla.app.ui.nav.Sell
import com.galla.app.ui.nav.SettingsScreenKey
import com.galla.app.ui.nav.StockIn
import com.galla.app.ui.theme.GallaRadius
import com.galla.app.ui.theme.GallaTheme
import com.galla.app.ui.theme.GallaType

/**
 * The Today tab: how the shop is doing right now. One big number (today's sales) on the
 * lime card, what needs restocking, the four things an owner does most, and what just happened.
 */
@Composable
fun TodayScreen() {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val vm: TodayViewModel = viewModel { TodayViewModel(app) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    ScreenColumn {
        // Shop name and date
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GallaLogo(44.dp)
            Column(Modifier.weight(1f)) {
                Text(ui.shopTitle, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Dates.weekdayDayMonth(ui.today), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            IconBtn(Ic.settings, "Settings", { nav.go(SettingsScreenKey) })
        }

        Gap(8.dp)

        // The hero: today's sales
        HeroCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Eyebrow("Today's sales", color = GallaTheme.extras.onHero.copy(alpha = 0.72f))
            Gap(2.dp)
            val sales = Money.format(ui.salesPaise)
            RollingText(sales, style = heroStyle(sales))
            Gap(4.dp)
            Text(
                if (ui.bills == 0) "No bills yet today"
                else "${ui.bills} ${if (ui.bills == 1) "bill" else "bills"} · " +
                    if (ui.profitPaise < 0L) "${Money.format(-ui.profitPaise)} loss so far" else "${Money.format(ui.profitPaise)} profit",
                style = MaterialTheme.typography.titleSmall,
            )
            Gap(18.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Btn("New sale", { nav.switchTab(Sell) }, icon = Ic.point_of_sale, style = BtnStyle.HERO, fillIcon = true)
                Btn("Scan", { nav.openIn(Sell, Scan("sell")) }, icon = Ic.qr_code_scanner, style = BtnStyle.TEXT)
            }
        }

        Gap(10.dp)

        // Two numbers side by side. Their corners lean towards each other.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                label = "Spent today",
                value = Money.format(ui.spentPaise),
                modifier = Modifier.weight(1f),
                icon = Ic.trending_down,
                shape = RoundedCornerShape(topStart = GallaRadius.l, topEnd = GallaRadius.xs, bottomEnd = GallaRadius.xs, bottomStart = GallaRadius.l),
                onClick = { nav.switchTab(MoneyTab) },
            )
            StatTile(
                label = "Stock worth",
                value = Money.format(ui.stockValuePaise),
                modifier = Modifier.weight(1f),
                kind = CardKind.INK,
                icon = Ic.inventory_2,
                shape = RoundedCornerShape(topStart = GallaRadius.xs, topEnd = GallaRadius.l, bottomEnd = GallaRadius.l, bottomStart = GallaRadius.xs),
            )
        }

        // Restock warning
        if (ui.restock.isNotEmpty()) {
            Gap(10.dp)
            val out = ui.restock.count { it.level == StockLevel.OUT }
            GCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                kind = if (out > 0) CardKind.ERROR else CardKind.WARN,
                onClick = { nav.go(Restock) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GIcon(Ic.production_quantity_limits, fill = true, size = 28.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${ui.restock.size} ${if (ui.restock.size == 1) "item needs" else "items need"} restocking",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            ui.restock.take(3).joinToString(", ") { it.name } + if (ui.restock.size > 3) " and more" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    GIcon(Ic.chevron_right)
                }
            }
        }

        // The four most common jobs
        SectionHeader("Quick actions")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionTile(Ic.add_box, "Add item", Modifier.weight(1f)) { nav.openIn(com.galla.app.ui.nav.Items, ItemEdit()) }
            ActionTile(Ic.local_shipping, "Stock in", Modifier.weight(1f)) { nav.go(StockIn()) }
            ActionTile(Ic.payments, "Expense", Modifier.weight(1f)) { nav.go(ExpenseAdd) }
        }

        // What just happened
        SectionHeader("Recent")
        if (ui.activity.isEmpty()) {
            GCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    if (ui.itemCount == 0) "Add your first item, then make a sale. It will show up here."
                    else "Nothing yet. Your sales, stock and expenses will show up here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
        } else {
            GGroup {
                ui.activity.forEachIndexed { index, row ->
                    GroupItem(
                        index = index,
                        count = ui.activity.size,
                        onClick = if (row.saleId != 0L) ({ nav.go(Receipt(row.saleId)) }) else null,
                    ) {
                        RowContent(
                            title = row.title,
                            subtitle = row.subtitle,
                            leading = {
                                ShapeIcon(
                                    row.icon,
                                    size = 40.dp,
                                    background = if (row.moneyIn) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                    tint = if (row.moneyIn) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                                )
                            },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        row.amount,
                                        style = GallaType.amount,
                                        color = if (row.moneyIn) GallaTheme.extras.good else scheme.onSurface,
                                    )
                                    if (row.saleId != 0L) Chevron()
                                }
                            },
                        )
                    }
                }
            }
        }

        Gap(10.dp)
        Row(
            modifier = Modifier.padding(start = 20.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GIcon(Ic.lock, size = 14.dp, tint = scheme.onSurfaceVariant, fill = true)
            Text("Everything stays on this phone.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Foot("")
    }
}

/** The big number shrinks a little when the amount is long, so it always fits on one line. */
private fun heroStyle(text: String) = when {
    text.length <= 7 -> GallaType.hero
    text.length <= 9 -> GallaType.hero.copy(fontSize = GallaType.hero.fontSize * 0.82f, lineHeight = GallaType.hero.lineHeight * 0.82f)
    text.length <= 11 -> GallaType.hero.copy(fontSize = GallaType.hero.fontSize * 0.68f, lineHeight = GallaType.hero.lineHeight * 0.68f)
    else -> GallaType.hero.copy(fontSize = GallaType.hero.fontSize * 0.56f, lineHeight = GallaType.hero.lineHeight * 0.56f)
}

/** A big square-ish button with an icon above a word. */
@Composable
private fun ActionTile(icon: Ic, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GCard(modifier = modifier, kind = CardKind.SECONDARY, onClick = onClick, padding = 14.dp) {
        GIcon(icon, size = 26.dp, fill = true)
        Gap(18.dp)
        Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
