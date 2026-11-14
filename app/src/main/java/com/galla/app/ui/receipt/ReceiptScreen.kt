package com.galla.app.ui.receipt

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.galla.app.AppContainer
import com.galla.app.core.util.Dates
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.data.local.db.SaleEntity
import com.galla.app.data.local.db.SaleLineEntity
import com.galla.app.hardware.Printer
import com.galla.app.ui.components.AmountRow
import com.galla.app.ui.components.AppBar
import com.galla.app.ui.components.Btn
import com.galla.app.ui.components.BtnSize
import com.galla.app.ui.components.BtnStyle
import com.galla.app.ui.components.EmptyState
import com.galla.app.ui.components.Gap
import com.galla.app.ui.components.ReceiptShape
import com.galla.app.ui.components.ScreenColumn
import com.galla.app.ui.components.ShapeIcon
import com.galla.app.ui.components.asState
import com.galla.app.ui.icons.Ic
import com.galla.app.ui.nav.LocalContainer
import com.galla.app.ui.nav.LocalMessenger
import com.galla.app.ui.nav.LocalNav
import com.galla.app.ui.nav.Receipt
import com.galla.app.ui.nav.Sell
import com.galla.app.ui.theme.GallaTheme
import com.galla.app.ui.theme.GallaType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

data class ReceiptUi(
    val loaded: Boolean = false,
    val sale: SaleEntity? = null,
    val lines: List<SaleLineEntity> = emptyList(),
    val shopName: String = "",
)

class ReceiptViewModel(app: AppContainer, saleId: Long) : ViewModel() {
    val state: StateFlow<ReceiptUi> =
        combine(app.shop.sale(saleId), app.shop.saleLines(saleId), app.settings.settings) { sale, lines, settings ->
            ReceiptUi(true, sale, lines, settings.shopName)
        }.asState(this, ReceiptUi())
}

/**
 * A saved bill, drawn as a paper receipt with a torn bottom edge. Right after a sale it
 * opens with a "done" moment; later it is how an old bill is looked up, printed or sent.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReceiptScreen(route: Receipt) {
    val app = LocalContainer.current
    val nav = LocalNav.current
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val vm: ReceiptViewModel = viewModel(key = "receipt-${route.saleId}") { ReceiptViewModel(app, route.saleId) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val sale = ui.sale

    ScreenColumn {
        AppBar(onBack = { nav.back() }, title = if (route.fresh) "" else "Bill no. ${route.saleId}")

        if (sale == null) {
            if (ui.loaded) EmptyState("Bill not found", "It may have been removed.", Ic.search_off)
            return@ScreenColumn
        }

        if (route.fresh) {
            // The "done" moment: a tick on a sunny shape that springs into place.
            val pop = remember { Animatable(0.4f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f)) }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ShapeIcon(
                    Ic.check,
                    modifier = Modifier.scale(pop.value),
                    size = 92.dp,
                    shape = MaterialShapes.Sunny,
                    background = GallaTheme.extras.hero,
                    tint = GallaTheme.extras.onHero,
                )
                Gap(12.dp)
                Text("Sale saved", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${Money.format(sale.totalPaise)} · bill no. ${sale.id}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
            Gap(20.dp)
        }

        // The paper
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(ReceiptShape())
                .background(if (GallaTheme.dark) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest)
                .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 34.dp),
        ) {
            Text(
                ui.shopName.ifBlank { "Receipt" },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Bill no. ${sale.id} · ${Dates.full(sale.at)}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = scheme.outlineVariant)
            ui.lines.forEach { line ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(line.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${Qty.withUnit(line.qtyMilli, line.unit)} × ${Money.format(line.pricePaise)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Text(Money.format(line.totalPaise), style = GallaType.amount)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = scheme.outlineVariant)
            AmountRow("Total", Money.format(sale.totalPaise), strong = true)
        }
        Gap(18.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Btn(
                "Print",
                {
                    val ok = Printer.print(context, "Bill ${sale.id}", Printer.receiptBitmap(ui.shopName, sale, ui.lines))
                    if (!ok) messenger.show("This phone cannot print.")
                },
                Modifier.weight(1f),
                icon = Ic.print,
                style = BtnStyle.TONAL,
            )
            Btn(
                "Send",
                { Printer.shareText(context, "Bill no. ${sale.id}", Printer.receiptText(ui.shopName, sale, ui.lines)) },
                Modifier.weight(1f),
                icon = Ic.share,
                style = BtnStyle.TONAL,
            )
        }
        if (route.fresh) {
            Gap(10.dp)
            Btn(
                "Next customer",
                { nav.switchTab(Sell) },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                icon = Ic.point_of_sale,
                size = BtnSize.LG,
            )
        }
    }
}
